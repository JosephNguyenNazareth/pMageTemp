package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.casestudy.PreDefinedArtifactInstance;
import com.pmsconnect.mage.config.AppConfig;
import com.pmsconnect.mage.utils.ActionEvent;
import com.pmsconnect.mage.utils.Alignment;

import com.pmsconnect.mage.utils.Artifact;
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
                    this.retrieveLatestTrigger(connector, monitoringMess);
                    this.notifyViolatedTrigger(connector, monitoringMess);
                }

                // TODO: checking pms log to detect manual pms updates
                this.checkingPMSLog(connector);

                connector.addMonitoringLog(monitoringMess.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void notifyViolatedTrigger(Connector connector, StringBuilder monitoringMess) {
        for (Alignment align : connector.getHistoryTriggerList()) {
            if (align.getViolated().equals(false))
                alertTriggeredAction(align.getTriggeredActionId(), connector, monitoringMess);
        }
    }


    public List<Dictionary<String, String>> getAllTrigger(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        String configPath =  connector.getBridge().getAppConfig();
        AppConfig appConfig = new AppConfig(configPath);
        appConfig.setProjectLink(connector.getBridge().getProjectLink());
        return appConfig.getLatestTrigger(true, connector.getActionEventTable(), connector.getBridge());
    }


    public void retrieveLatestTrigger(Connector connector, StringBuilder monitoringMess) {
        connector.getAppConfig().setProjectLink(connector.getBridge().getProjectLink());
        List<Dictionary<String, String>> actionTriggeredList = connector.getAppConfig().getLatestTrigger(false, connector.getActionEventTable(), connector.getBridge());

        Dictionary<String, String> triggeredAction = actionTriggeredList.get(0);
        String triggeredActionTask = triggeredAction.get("task");
        String actionTriggeredId = triggeredAction.get("id");
        String triggeredTime = triggeredAction.get("time");

        // if the latest triggeredAction is already in the list of retrieved triggeredAction, skip ths later work
        if (connector.findTriggeredActionId(actionTriggeredId) != null) {
            String currentMonitoringMessage = "Project is up-to-date";
            monitoringMess.append(currentMonitoringMessage);
            String[] updatePMS = currentProcessInstanceState(connector, monitoringMess);
            if (updatePMS != null) {
                boolean noViolated = compareLatestUpdate(connector, updatePMS);
                if (!noViolated) {
                    connector.addHistoryTriggerList(new Alignment("", updatePMS[0], "", updatePMS[1], true, "", currentMonitoringMessage));
                }
            }
            return;
        }

        // skip validating the triggeredAction if the connector's owner is not the actor
        String actor = triggeredAction.get("actor");
        if (!actor.equals(connector.getBridge().getUserNameApp()))
            return;

        String[] updatePMS = currentProcessInstanceState(connector, monitoringMess);
        String taskFound = detectTaskFromTriggeredAction(connector, triggeredActionTask, monitoringMess);

        // skip reverted commit
//            if (taskFound.equals("revert")) {
//                detectRevertedTriggeredAction(triggeredActionTask, connector);
//            } else
        if (taskFound.equals("unknown")) {
            monitoringMess.append("Task unknown\n");
            alertTriggeredAction(actionTriggeredId, connector, monitoringMess);
        } else {
            validateTask(connector, triggeredActionTask, taskFound, actionTriggeredId, monitoringMess);
        }

        if (updatePMS != null) {
            boolean noViolated = compareLatestUpdate(connector, updatePMS);
            if (!noViolated) {
                connector.addHistoryTriggerList(new Alignment(actionTriggeredId, updatePMS[0], triggeredTime, updatePMS[1], false, taskFound, monitoringMess.toString()));
            }
        }

        connectorRepository.save(connector);

    }

    private void detectRevertedTriggeredAction(String extraInfo, Connector connector) {
        String revertedTriggeredActionId = extraInfo.substring(extraInfo.indexOf("This reverts commit ")).replace(".","").trim();

        connector.findTriggeredActionId(revertedTriggeredActionId).setViolated(false);
    }

    public String detectTaskFromTriggeredAction(Connector connector, String triggeredActionTask, StringBuilder monitoringMess) {
        // TermDetect termDetector = new TermDetect();
        // return caseStudy.checkRelevant(commitMessage, termDetector);
        // cannot use this term detector in this use case, must build another system
        String taskDetect = "";

        // skip revert commit
        if (triggeredActionTask.contains("Revert"))
            return "revert";

        boolean found = false;
        for (ActionEvent actionEvent : connector.getActionEventTable()) {
            if (triggeredActionTask.contains(actionEvent.getActionDetail())) {
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

    public List<PreDefinedArtifactInstance> detectArtifactFromTriggeredAction(Connector connector, String triggeredActionTask, StringBuilder monitoringMess) {
        List<PreDefinedArtifactInstance> preDefinedArtifactInstanceList = new ArrayList<>();
        if (triggeredActionTask.contains(";")) {
            String importantMessage = triggeredActionTask.contains("|") ? triggeredActionTask.substring(0, triggeredActionTask.indexOf("|")) : triggeredActionTask;
            String[] terms = importantMessage.split(";");
            for (int i = 1; i < terms.length; i++) {
                String[] artifact = terms[i].split(":");
                if (artifact.length < 2)
                    monitoringMess.append("Invalid syntax. Cannot detect artifact\n");

                if (possibleAddingArtifact(connector, artifact[0]))
                    preDefinedArtifactInstanceList.add(new PreDefinedArtifactInstance(artifact[0], artifact[1], Integer.parseInt(artifact[2])));
            }
        }
        return preDefinedArtifactInstanceList;
    }

    public void validateTask(Connector connector, String triggeredActionTask, String taskDetected, String actionTriggeredId, StringBuilder monitoringMess) {
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
                    monitoringMess.append("Triggered action is validated. Task is launched\n");
                    completeTaskTriggered(connector, taskDetected, triggeredActionTask, monitoringMess);
                    connectorRepository.save(connector);
                } else {
                    // TODO: AI-augmented experience should provide users with more profound helpful info
                    monitoringMess.append("Task corresponding with triggered action ").append(actionTriggeredId).append(" not found by the process instance" + connector.getBridge().getProcessId() + "\n");
                    alertTriggeredAction(actionTriggeredId, connector, monitoringMess);
                }
//
//                int isPermitted = Integer.parseInt(responseBody);
//
//                if (isPermitted == -1) {
//                    monitoringMess.append("Task corresponding with commit " + actionTriggeredId + " not found\n");
//                    revertCommit(actionTriggeredId, connector, monitoringMess);
//                }
//                else if (isPermitted == 0) {
//                    monitoringMess.append("Commit is invalidated. No task is launched\n");
//                    revertCommit(actionTriggeredId, connector, monitoringMess);
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

    public void alertTriggeredAction(String actionTriggeredId, Connector connector, StringBuilder monitoringMess) {
//        String configPath =  "./src/main/resources/app_config.json";
//        Retriever retriever = new Retriever(configPath);
//        retriever.setRepoLink(connector.getUserRepo().getRepoLink());

        connector.findTriggeredActionId(actionTriggeredId).setViolated(true);
//        boolean reverted = retriever.revertCommit(actionTriggeredId);

        monitoringMess.append("Triggered action " + actionTriggeredId + "is not validated. Please relaunch the task.\n");
    }

    private void completeTaskTriggered(Connector connector, String taskDetected, String triggeredActionTask, StringBuilder monitoringMess) {
        String newTaskInstanceId = startTaskInstance(connector, taskDetected);
        List<PreDefinedArtifactInstance> detectedArtifacts = detectArtifactFromTriggeredAction(connector, triggeredActionTask, monitoringMess);
        endTaskInstance(connector, newTaskInstanceId, detectedArtifacts);
        updateArtifactPool(connector, detectedArtifacts);
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
            String lastUpdateTimePMage = connector.getHistoryTriggerList().get(connector.getHistoryTriggerList().size() - 1).getProcessInstanceChangeTime();
            return lastUpdateTimePMage.equals(lastUpdateTimePMS);
        }
        return false;
    }

    public boolean possibleAddingArtifact(Connector connector, String detectedArtifactName) {
        Connector baseConnector = connectorRepository.findById(((SupplementaryConnector) connector).getSuppConnectorId()).orElseThrow(() -> new IllegalStateException("Connector with id " + ((SupplementaryConnector) connector).getSuppConnectorId() + "does not exist."));
        Map<String, Artifact> mainArtifactPool = connector.getArtifactPool();
        Map<String, Artifact> monitoredArtifactPool = baseConnector.getArtifactPool();

        if (monitoredArtifactPool.containsKey(detectedArtifactName))
            if (monitoredArtifactPool.get(detectedArtifactName).isAvailable())
                return true;
        else
            return true;
        return false;
    }

    public void updateArtifactPool(Connector connector, List<PreDefinedArtifactInstance> detectedArtifacts) {
        if (connector instanceof SupplementaryConnector) {
            Connector baseConnector = connectorRepository.findById(((SupplementaryConnector) connector).getSuppConnectorId()).orElseThrow(() -> new IllegalStateException("Connector with id " + ((SupplementaryConnector) connector).getSuppConnectorId() + "does not exist."));
            Map<String, Artifact> mainArtifactPool = connector.getArtifactPool();
            Map<String, Artifact> monitoredArtifactPool = baseConnector.getArtifactPool();

            for (PreDefinedArtifactInstance artifact: detectedArtifacts) {
                String detectedArtifactName = artifact.getName();
                if (monitoredArtifactPool.containsKey(detectedArtifactName))
                    if (monitoredArtifactPool.get(detectedArtifactName).isAvailable())
                        if (mainArtifactPool.containsKey(detectedArtifactName))
                            mainArtifactPool.get(detectedArtifactName).setAvailable(true);
                        else
                            mainArtifactPool.put(detectedArtifactName, new Artifact(detectedArtifactName, true));
                    else
                        throw new IllegalStateException("Cannot update artifact due to conflicts of monitored connector " + ((SupplementaryConnector) connector).getSuppConnectorId());
                else
                    if (mainArtifactPool.containsKey(detectedArtifactName))
                        mainArtifactPool.get(detectedArtifactName).setAvailable(true);
                    else
                        mainArtifactPool.put(detectedArtifactName, new Artifact(detectedArtifactName, true));
            }
        } else {
            Map<String, Artifact> artifactPool = connector.getArtifactPool();
            for (PreDefinedArtifactInstance artifact: detectedArtifacts) {
                String detectedArtifactName = artifact.getName();
                if (artifactPool.containsKey(detectedArtifactName))
                    artifactPool.get(detectedArtifactName).setAvailable(true);
                else
                    artifactPool.put(detectedArtifactName, new Artifact(detectedArtifactName, true));
            }
        }

        connectorRepository.save(connector);
    }

    public void checkingPMSLog(Connector connector) {
        // retrieve pms log
        HttpClient client = HttpClients.createDefault();
        String log = "";
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());

            String finalUri = connector.getPmsConfig().buildAPI("log", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());
                if (connector.getBridge().getPmsName().equals("core-bape"))
                    log = content;
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        // detect manual update
        // if not, ignore
        // if yes, retrieve the update (mostly complete the task)
        // check if the task related artifact is in the artifact pool

    }
}
