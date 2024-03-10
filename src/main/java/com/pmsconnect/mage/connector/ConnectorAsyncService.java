package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.casestudy.PreDefinedArtifactInstance;
import com.pmsconnect.mage.config.Retriever;
import com.pmsconnect.mage.utils.ActionEvent;
import com.pmsconnect.mage.utils.Alignment;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
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
//        this.openProcess(connector);

        StringBuilder monitoringMessAll = new StringBuilder();
        monitoringMessAll.append("Fresh monitoring connector " + connector.getId() + " of project id" + connector.getBridge().getProcessId() + "\n");
        // just in case, retrieve all the commits of this project
//        if (connector.existActionEventType("commit-pushed"))
//            this.retrieveAllCommit(connector, monitoringMessAll);
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
                monitoringMess.append("Monitoring connector " + connector.getId() + " of project id" + connector.getBridge().getProcessId() + " of user " + connector.getBridge().getProcessId() + "\n");

                if (connector.existActionEventType("commit-pushed")) {
                    this.retrieveLatestCommit(connector, monitoringMess);
                    this.notifyViolatedCommit(connector, monitoringMess);
                }

                connector.addMonitoringLog(monitoringMess.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void notifyViolatedCommit(Connector connector, StringBuilder monitoringMess) {
        for (Alignment align : connector.getHistoryCommitList()) {
            if (align.getViolated())
                alertCommit(align.getCommitId(), connector, monitoringMess);
        }
    }


    public List<Dictionary<String, String>> getAllCommit(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        String configPath =  connector.getBridge().getAppConfig();
        Retriever retriever = new Retriever(configPath);
        retriever.setRepoLink(connector.getBridge().getProjectLink());
        return retriever.getLatestCommitLog(true);
    }


    public void retrieveLatestCommit(Connector connector, StringBuilder monitoringMess) {
        connector.getRetriever().setRepoLink(connector.getBridge().getProjectLink());
        List<Dictionary<String, String>> commitList = connector.getRetriever().getLatestCommitLog(false);

        Dictionary<String, String> commit = commitList.get(0);
        String commitMessage = commit.get("title");
        String commitId = commit.get("id");
        String commitTime = commit.get("time");

        // if the latest commit is already in the list of retrieved commit, skip ths later work
        if (connector.findCommitId(commitId) != null) {
            String currentMonitoringMessage = "Commit is up-to-date";
            monitoringMess.append(currentMonitoringMessage);
            String[] updatePMS = currentProcessInstanceState(connector, monitoringMess);
            if (updatePMS != null) {
                boolean noViolated = compareLatestUpdate(connector, updatePMS);
                if (!noViolated) {
                    connector.addHistoryCommitList(new Alignment("", updatePMS[0], "", updatePMS[1], true, "", currentMonitoringMessage));
                }
            }
            return;
        }

        // skip validating the commit if the connector's owner is not the committer
        String committerName = commit.get("committer_name");
        if (!committerName.equals(connector.getBridge().getUserNameApp()))
            return;

        String[] updatePMS = currentProcessInstanceState(connector, monitoringMess);
        String taskFound = detectTaskFromCommit(connector, commitMessage, monitoringMess);

        // skip reverted commit
        if (taskFound.equals("revert")) {
            detectRevertedCommit(commitMessage, connector);
        }
        else if (taskFound.equals("unknown")) {
            monitoringMess.append("Task unknown\n");
            alertCommit(commitId, connector, monitoringMess);
        } else {
            validateCommit(connector, commitMessage, taskFound, commitId, monitoringMess);
        }

        if (updatePMS != null) {
            boolean noViolated = compareLatestUpdate(connector, updatePMS);
            if (!noViolated) {
                connector.addHistoryCommitList(new Alignment(commitId, updatePMS[0], commitTime, updatePMS[1], false, taskFound, monitoringMess.toString()));
            }
        }

        connectorRepository.save(connector);
    }

    private void detectRevertedCommit(String commitMessage, Connector connector) {
        String revertedCommitId = commitMessage.substring(commitMessage.indexOf("This reverts commit ")).replace(".","").trim();

        connector.findCommitId(revertedCommitId).setViolated(false);
    }

    public String detectTaskFromCommit(Connector connector, String commitMessage, StringBuilder monitoringMess) {
        // TermDetect termDetector = new TermDetect();
        // return caseStudy.checkRelevant(commitMessage, termDetector);
        // cannot use this term detector in this use case, must build another system
        String taskDetect = "";

        // skip revert commit
        if (commitMessage.contains("Revert"))
            return "revert";

        boolean found = false;
        for (ActionEvent actionEvent : connector.getActionEventTable()) {
            if (commitMessage.contains(actionEvent.getActionDetail())) {
                found = true;
                taskDetect = actionEvent.getTask();
                break;
            }
        }

        if (!found)
            taskDetect = "unknown";

//        if (commitMessage.contains("end task") || commitMessage.contains("finish task")) {
//            if (commitMessage.contains("|"))
//                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5, commitMessage.indexOf("|"));
//            else if (commitMessage.contains(";"))
//                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5,  commitMessage.indexOf(";"));
//            else
//                taskDetect = commitMessage.substring(commitMessage.indexOf("task") + 5);
//        } else {
//            taskDetect = "unknown";
//        }
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
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());
            paramMap.put("taskName", taskDetected);
            paramMap.put("actorName", connector.getBridge().getUserNamePms());

            String finalUri = connector.getPmsConfig().buildAPI("validateTask", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String responseBody = EntityUtils.toString(getResponse.getEntity());
                if (responseBody.length() > 0) {
                    monitoringMess.append("Commit is validated. Task is launched\n");
                    completeTaskCommitted(connector, taskDetected, commitMessage, monitoringMess);
                    connectorRepository.save(connector);
                } else {
                    // TODO: AI-aumengted experience should provide users with more profound helpful info
                    monitoringMess.append("Task corresponding with commit ").append(commitId).append(" not found by the process instance" + connector.getBridge().getProcessId() + "\n");
                    alertCommit(commitId, connector, monitoringMess);
                }
//
//                int isPermitted = Integer.parseInt(responseBody);
//
//                if (isPermitted == -1) {
//                    monitoringMess.append("Task corresponding with commit " + commitId + " not found\n");
//                    revertCommit(commitId, connector, monitoringMess);
//                }
//                else if (isPermitted == 0) {
//                    monitoringMess.append("Commit is invalidated. No task is launched\n");
//                    revertCommit(commitId, connector, monitoringMess);
//                } else if (isPermitted == 1){
//                    monitoringMess.append("Commit is validated. Task is launched\n");
//                    completeTaskCommitted(connector, taskDetected, commitMessage, monitoringMess);
//                    connectorRepository.save(connector);
//                } else if (isPermitted == 2) {
//                    monitoringMess.append("Task has been completed.");
//                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void alertCommit(String commitId, Connector connector, StringBuilder monitoringMess) {
//        String configPath =  "./src/main/resources/app_config.json";
//        Retriever retriever = new Retriever(configPath);
//        retriever.setRepoLink(connector.getUserRepo().getRepoLink());

        connector.findCommitId(commitId).setViolated(true);
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
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());
            paramMap.put("taskName", taskDetected);
            paramMap.put("actorName", connector.getBridge().getUserNamePms());

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
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());
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
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());
            paramMap.put("processInstanceState", "false");

            String finalUri = connector.getPmsConfig().buildAPI("changeProcessState", urlMap, paramMap);
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot open process instance id " + connector.getBridge().getProcessId());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] currentProcessInstanceState(Connector connector, StringBuilder monitoringMess) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());

            String finalUri = connector.getPmsConfig().buildAPI("verify", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(content);
                JSONObject contentJSON = (JSONObject) obj;

                JSONArray updateDetail = (JSONArray) contentJSON.get("updateDetail");
                JSONObject objUpdate = (JSONObject) updateDetail.get(updateDetail.size() - 1);
                String lastUpdateTimePMS = objUpdate.keySet().toArray()[0].toString();
                String lastUpdatePMS = objUpdate.get(lastUpdateTimePMS).toString();

                return new String[]{lastUpdateTimePMS, lastUpdatePMS};
            }
        } catch (URISyntaxException | IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private boolean compareLatestUpdate(Connector connector, String[] updatePMS) {
        if (updatePMS != null) {
            String lastUpdateTimePMS = updatePMS[0];
            String lastUpdateTimePMage = connector.getHistoryCommitList().get(connector.getHistoryCommitList().size() - 1).getProcessInstanceChangeTime();
            return lastUpdateTimePMage.equals(lastUpdateTimePMS);
        }
        return false;
    }
}
