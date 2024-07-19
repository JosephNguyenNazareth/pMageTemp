package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.config.PmsConfig;
import com.pmsconnect.mage.config.AppConfig;
import com.pmsconnect.mage.user.Bridge;
import com.pmsconnect.mage.utils.ActionEvent;
import com.pmsconnect.mage.utils.Alignment;
import com.pmsconnect.mage.utils.Artifact;
import com.pmsconnect.mage.utils.TaskArtifact;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "connector")
public class Connector {
    @Id
    private String id;
    private Bridge bridge;
    private List<ActionEvent> actionEventTable;
    private String actionEventDescription;
    private List<Alignment> historyTriggerList;
    private Map<String, String> monitoringLog;
    private boolean monitoring;
    private AppConfig appConfig;
    private PmsConfig pmsConfig;
    private String userName;
    private Map<String, Artifact> artifactPool;
    private List<TaskArtifact> taskArtifactList;

    public Connector() {
        this.loadProperties();
    }

    public void loadProperties() {
        try {
            InputStream file = Connector.class.getResourceAsStream("/application.properties");
            if (file!=null) System.getProperties().load(file);
        } catch (IOException e) {
            throw new RuntimeException("Error loading application.properties", e);
        }
    }

    public Connector(Bridge bridge) {
        this.id = UUID.randomUUID().toString();
        this.historyTriggerList = new ArrayList<>();
        this.monitoringLog = new HashMap<>();
        this.actionEventTable = new ArrayList<>();
        this.monitoring = false;
        this.bridge = bridge;
        this.loadProperties();
        this.appConfig = new AppConfig(System.getProperty("appconfig"));
        this.pmsConfig = new PmsConfig(System.getProperty("pmsconfig"), this.getBridge().getPmsName());
        this.artifactPool = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public List<Alignment> getHistoryTriggerList() {
        return historyTriggerList;
    }

    public void setHistoryTriggerList(List<Alignment> historyTriggerList) {
        this.historyTriggerList = historyTriggerList;
    }

    public void addHistoryTriggerList(String historyCommit, String processInstanceChange, String commitTime, String changeTime, Boolean isViolated, String taskFound, String monitoringMessage) {
        this.historyTriggerList.add(new Alignment(historyCommit, processInstanceChange, commitTime, changeTime, isViolated, taskFound, monitoringMessage));
    }

    public void addHistoryTriggerList(String historyCommit, String commitTime, Boolean isViolated) {
        this.historyTriggerList.add(new Alignment(historyCommit, "", commitTime, "", isViolated, "", ""));
    }

    public void addHistoryTriggerList(Alignment alignment) {
        this.historyTriggerList.add(alignment);
    }

    public Alignment findTriggeredActionId(String actionId) {
        for (Alignment alignment: this.historyTriggerList) {
            if (alignment.getTriggeredActionId().equals(actionId))
                return alignment;
        }
        return null;
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    public void setMonitoring(boolean monitoring) {
        this.monitoring = monitoring;
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public PmsConfig getPmsConfig() {
        return pmsConfig;
    }

    public void setPmsConfig(PmsConfig pmsConfig) {
        this.pmsConfig = pmsConfig;
    }

    public Map<String, String> getMonitoringLog() {
        return monitoringLog;
    }

    public void addMonitoringLog(String monitoringMess) {
        this.monitoringLog.put(LocalDateTime.now().toString(), monitoringMess);
    }

    public void setMonitoringLog(Map<String, String> monitoringLog) {
        this.monitoringLog = monitoringLog;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }

    public List<ActionEvent> getActionEventTable() {
        return actionEventTable;
    }

    public void addActionEvent(ActionEvent actionEvent) {
        this.actionEventTable.add(actionEvent);
    }

    public void setActionEventTable(List<ActionEvent> actionEventTable) {
        this.actionEventTable = actionEventTable;
    }

    public String getActionEventDescription() {
        return actionEventDescription;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setActionEventDescription(String actionEventDescription) {
        this.actionEventDescription = actionEventDescription;
    }

    public Map<String, Artifact> getArtifactPool() {
        return artifactPool;
    }

    public void setArtifactPool(Map<String, Artifact> artifactPool) {
        this.artifactPool = artifactPool;
    }

    public void addArtifact(Artifact artifact) {
        this.artifactPool.put(artifact.getName(), artifact);
    }

    public void addArtifact(String name) {
        this.artifactPool.put(name, new Artifact(name));
    }

    public boolean existActionEventType(String eventType) {
        for (ActionEvent actionEvent : this.getActionEventTable()) {
            if (actionEvent.getEvent().equals(eventType))
                return true;
        }
        return false;
    }

    public void updateConfig() {
        this.pmsConfig.readConfig();
        this.appConfig.readConfig();
    }

    public List<TaskArtifact> getTaskArtifactList() {
        return taskArtifactList;
    }

    public void setTaskArtifactList(List<TaskArtifact> taskArtifactList) {
        this.taskArtifactList = taskArtifactList;
    }

    public void updateTaskArtifactList(List<String> taskArtifactStringList) {
        for (String taskArtifactString : taskArtifactStringList) {
            String[] taskArtifactStringParse = taskArtifactString.split(" : ");
            String[] input = taskArtifactStringParse[0].split(",");
            String taskName = taskArtifactStringParse[1];
            String[] output = taskArtifactStringParse[2].split(",");

            this.taskArtifactList.add(new TaskArtifact(taskName, input, output, "string"));
        }
    }
}
