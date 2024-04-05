package com.pmsconnect.mage.utils;

public class Alignment {
    private String commitId;
    private String processInstanceChange;
    private String commitTime;
    private String processInstanceChangeTime;
    private String taskFound;
    private String monitoringMessage;
    private Boolean violated;

    public Alignment() {
    }

    public Alignment(String commitId, String processInstanceChange, String commitTime, String processInstanceChangeTime, Boolean isViolated, String taskFound, String monitoringMessage) {
        this.commitId = commitId;
        this.processInstanceChange = processInstanceChange;
        this.commitTime = commitTime;
        this.processInstanceChangeTime = processInstanceChangeTime;
        this.violated = isViolated;
        this.taskFound = taskFound;
        this.monitoringMessage = monitoringMessage;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getProcessInstanceChange() {
        return processInstanceChange;
    }

    public void setProcessInstanceChange(String processInstanceChange) {
        this.processInstanceChange = processInstanceChange;
    }

    public String getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(String commitTime) {
        this.commitTime = commitTime;
    }

    public String getProcessInstanceChangeTime() {
        return processInstanceChangeTime;
    }

    public void setProcessInstanceChangeTime(String processInstanceChangeTime) {
        this.processInstanceChangeTime = processInstanceChangeTime;
    }

    public Boolean getViolated() {
        return violated;
    }

    public void setViolated(Boolean violated) {
        violated = violated;
    }

    public String getTaskFound() {
        return taskFound;
    }

    public void setTaskFound(String taskFound) {
        this.taskFound = taskFound;
    }

    public String getMonitoringMessage() {
        return monitoringMessage;
    }

    public void setMonitoringMessage(String monitoringMessage) {
        this.monitoringMessage = monitoringMessage;
    }
}
