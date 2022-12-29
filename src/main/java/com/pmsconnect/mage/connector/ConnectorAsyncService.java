package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.casestudy.PreDefinedArtifactInstance;
import com.pmsconnect.mage.config.PmsConfig;
import com.pmsconnect.mage.config.Retriever;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

@Service
public class ConnectorAsyncService {
    private final ConnectorRepository connectorRepository;

    @Autowired
    public ConnectorAsyncService (ConnectorRepository mageRepository) {
        this.connectorRepository = mageRepository;
    }

    @Async
    public void monitorProcessInstance(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        if (!connector.isMonitoring()) {
            connector.setMonitoring(true);
            connectorRepository.save(connector);
            this.watchProject(connector);
        } else {
            throw new IllegalStateException("This connector is already monitored");
        }
    }


    public void watchProject(Connector connector) {
        // allow process in pms to be run
        this.openProcess(connector);

        StringBuilder monitoringMessAll = new StringBuilder();
        monitoringMessAll.append("Fresh monitoring connector " + connector.getId() + " of project id" + connector.getUserPMage().getProjectId() + " of user " + connector.getUserPMage().getUserName() + "\n");
        // just in case, retrieve all the commits of this project
        this.retrieveAllCommit(connector, monitoringMessAll);
        connector.addMonitoringLog(monitoringMessAll.toString());

        // then run this as a background service to check commit status
        while(true) {
            try {
                // check last commit every 10 seconds
                Thread.sleep(10000);

                // check if there is a stop monitoring request
                Connector updatedConnector = connectorRepository.findById(connector.getId()).orElseThrow(() -> new IllegalStateException("Connector with id " + connector.getId() + "does not exist."));
                if (!updatedConnector.isMonitoring())
                    return;

                StringBuilder monitoringMess = new StringBuilder();
                monitoringMess.append("Monitoring connector " + connector.getId() + " of project id" + connector.getUserPMage().getProjectId() + " of user " + connector.getUserPMage().getUserName() + "\n");
                this.retrieveLatestCommit(connector, monitoringMess);

                this.notifyViolatedCommit(connector, monitoringMess);

                connector.addMonitoringLog(monitoringMess.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void notifyViolatedCommit(Connector connector, StringBuilder monitoringMess) {
        for (String commitId : connector.getViolatedCommitList()) {
            revertCommit(commitId, connector, monitoringMess);
        }
    }

    public void retrieveAllCommit(Connector connector, StringBuilder monitoringMess) {
        connector.getRetriever().setRepoLink(connector.getUserPMage().getRepoRemote());
        List<Dictionary<String, String>> commitList = connector.getRetriever().getLatestCommitLog(true);

        for (Dictionary<String, String> commit : commitList) {
            String commitMessage = commit.get("title");
            String commitId = commit.get("id");

            // if this commit is already in the history commit log of that connection
            if (connector.getHistoryCommitList().contains(commitId))
                continue;

            // skip validating the commit if the connector's owner is not the committer
            String committerName = commit.get("committer_name");
            if (!committerName.equals(connector.getUserPMage().getUserName()))
                continue;

            connector.addHistoryCommitList(commitId);
            String taskFound = detectTaskFromCommit(commitMessage, monitoringMess);

            // skip reverted commit
            if (taskFound.equals("revert")) {
                detectRevertedCommit(commitMessage, connector);
            }
            else if (taskFound.equals("unknown")) {
                monitoringMess.append("Task unknown\n");
                revertCommit(commitId, connector, monitoringMess);
            } else {
                validateCommit(connector, commitMessage, taskFound, commitId, monitoringMess);
            }
        }
        connectorRepository.save(connector);
    }

    public List<Dictionary<String, String>> getAllCommit(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        String configPath =  "./src/main/resources/repo_config.json";
        Retriever retriever = new Retriever(configPath);
        retriever.setRepoLink(connector.getUserPMage().getRepoRemote());
        return retriever.getLatestCommitLog(true);
    }


    public void retrieveLatestCommit(Connector connector, StringBuilder monitoringMess) {
        connector.getRetriever().setRepoLink(connector.getUserPMage().getRepoRemote());
        List<Dictionary<String, String>> commitList = connector.getRetriever().getLatestCommitLog(false);

        Dictionary<String, String> commit = commitList.get(0);
        String commitMessage = commit.get("title");
        String commitId = commit.get("id");

        // if the latest commit is already in the list of retrieved commit, skip ths later work
        if (connector.getHistoryCommitList().contains(commitId)) {
            monitoringMess.append("Commit is up-to-date");
            return;
        }

        // skip validating the commit if the connector's owner is not the committer
        String committerName = commit.get("committer_name");
        if (!committerName.equals(connector.getUserPMage().getUserName()))
            return;

        connector.addHistoryCommitList(commitId);
        String taskFound = detectTaskFromCommit(commitMessage, monitoringMess);

        // skip reverted commit
        if (taskFound.equals("revert")) {
            detectRevertedCommit(commitMessage, connector);
        }
        else if (taskFound.equals("unknown")) {
            monitoringMess.append("Task unknown\n");
            revertCommit(commitId, connector, monitoringMess);
        } else {
            validateCommit(connector, commitMessage, taskFound, commitId, monitoringMess);
        }

        connectorRepository.save(connector);
    }

    private void detectRevertedCommit(String commitMessage, Connector connector) {
        String revertedCommitId = commitMessage.substring(commitMessage.indexOf("This reverts commit ")).replace(".","").trim();

        connector.getViolatedCommitList().remove(revertedCommitId);
    }

    public String detectTaskFromCommit(String commitMessage, StringBuilder monitoringMess) {
        // TermDetect termDetector = new TermDetect();
        // return caseStudy.checkRelevant(commitMessage, termDetector);
        // cannot use this term detector in this use case, must build another system
        String taskDetect;

        // skip revert commit
        if (commitMessage.contains("Revert"))
            return "revert";

        if (commitMessage.contains("end task") || commitMessage.contains("finish task")) {
            if (commitMessage.contains("|"))
                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5, commitMessage.indexOf("|"));
            else if (commitMessage.contains(";"))
                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5,  commitMessage.indexOf(";"));
            else
                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5);
        } else {
            taskDetect = "unknown";
        }
        monitoringMess.append("Task detected: " + taskDetect + "\n");
        return taskDetect;
    }

    public List<PreDefinedArtifactInstance> detectArtifactFromCommit(String commitMessage, StringBuilder monitoringMess) {
        List<PreDefinedArtifactInstance> preDefinedArtifactInstanceList = new ArrayList<>();
        if (commitMessage.contains(";")) {
            String importantMessage = commitMessage.contains("|") ? commitMessage.substring(0, commitMessage.indexOf("|")) : commitMessage;
            String[] terms = importantMessage.split(";");
            for (int i = 1; i < terms.length; i++) {
                String[] artifact = terms[i].split(":");
                if (artifact.length < 2)
                    monitoringMess.append("Invalid syntax. Cannot detect artifact\n");
                preDefinedArtifactInstanceList.add(new PreDefinedArtifactInstance(artifact[0], artifact[1], Integer.parseInt(artifact[2])));
            }
        }
        return preDefinedArtifactInstanceList;
    }

    public void validateCommit(Connector connector, String commitMessage, String taskDetected, String commitId, StringBuilder monitoringMess) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("projectId", connector.getUserPMage().getProjectId());
            paramMap.put("taskName", taskDetected);
            paramMap.put("actorName", connector.getUserPMage().getRealName());

            String finalUri = connector.getPmsConfig().buildAPI("validateTask", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String responseBody = EntityUtils.toString(getResponse.getEntity());
                int isPermitted = Integer.parseInt(responseBody);

                if (isPermitted == -1) {
                    monitoringMess.append("Task corresponding with commit " + commitId + " not found\n");
                    revertCommit(commitId, connector, monitoringMess);
                }
                else if (isPermitted == 0) {
                    monitoringMess.append("Commit is invalidated. No task is launched\n");
                    revertCommit(commitId, connector, monitoringMess);
                } else if (isPermitted == 1){
                    monitoringMess.append("Commit is validated. Task is launched\n");
                    completeTaskCommitted(connector, taskDetected, commitMessage, monitoringMess);
                    connectorRepository.save(connector);
                } else if (isPermitted == 2) {
                    monitoringMess.append("Task has been completed.");
                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void revertCommit(String commitId, Connector connector, StringBuilder monitoringMess) {
//        String configPath =  "./src/main/resources/repo_config.json";
//        Retriever retriever = new Retriever(configPath);
//        retriever.setRepoLink(connector.getUserRepo().getRepoLink());

        connector.addViolatedCommitList(commitId);
//        boolean reverted = retriever.revertCommit(commitId);

        monitoringMess.append("Need reverting commit id " + commitId + "\n");
    }

    private void completeTaskCommitted(Connector connector, String taskDetected, String commitMessage, StringBuilder monitoringMess) {
        String newTaskInstanceId = startTaskInstance(connector, taskDetected);
        endTaskInstance(connector, newTaskInstanceId, detectArtifactFromCommit(commitMessage, monitoringMess));
    }

    private String startTaskInstance(Connector connector, String taskDetected) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("projectId", connector.getUserPMage().getProjectId());
            paramMap.put("taskName", taskDetected);
            paramMap.put("actorName", connector.getUserPMage().getRealName());

            String finalUri = connector.getPmsConfig().buildAPI("startTask", urlMap, paramMap);
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                return "";
            return EntityUtils.toString(getResponse.getEntity());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void endTaskInstance(Connector connector, String newTaskInstanceId, List<PreDefinedArtifactInstance> preDefinedArtifactInstanceList) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("projectId", connector.getUserPMage().getProjectId());
            paramMap.put("taskId", newTaskInstanceId);

            String finalUri = connector.getPmsConfig().buildAPI("endTask", urlMap, paramMap);
            HttpPut putMethod = new HttpPut(finalUri);
            putMethod.addHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(preDefinedArtifactInstanceList.toString(), "UTF-8");
            putMethod.setEntity(entity);

            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot end task id " + newTaskInstanceId);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openProcess(Connector connector) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("projectId", connector.getUserPMage().getProjectId());
            paramMap.put("processInstanceState", "false");

            String finalUri = connector.getPmsConfig().buildAPI("changeProjectState", urlMap, paramMap);
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot open process instance id " + connector.getUserPMage().getProjectId());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
