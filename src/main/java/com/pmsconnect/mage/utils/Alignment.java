package com.pmsconnect.mage.utils;

public class Alignment {
    private String triggeredActionId;
    private String processInstanceChange;
    private String triggeredTime;
    private String processInstanceChangeTime;
    private String taskFound;
    private String monitoringMessage;
    private Boolean violated;

    public Alignment() {
    }

    public Alignment(String triggeredActionId, String processInstanceChange, String triggeredTime, String processInstanceChangeTime, Boolean isViolated, String taskFound, String monitoringMessage) {
        this.triggeredActionId = triggeredActionId;
        this.processInstanceChange = processInstanceChange;
        this.triggeredTime = triggeredTime;
        this.processInstanceChangeTime = processInstanceChangeTime;
        this.violated = isViolated;
        this.taskFound = taskFound;
        this.monitoringMessage = monitoringMessage;
    }

    public String getTriggeredActionId() {
        return triggeredActionId;
    }

    public void setTriggeredActionId(String triggeredActionId) {
        this.triggeredActionId = triggeredActionId;
    }

    public String getProcessInstanceChange() {
        return processInstanceChange;
    }

    public void setProcessInstanceChange(String processInstanceChange) {
        this.processInstanceChange = processInstanceChange;
    }

    public String getTriggeredTime() {
        return triggeredTime;
    }

    public void setTriggeredTime(String triggeredTime) {
        this.triggeredTime = triggeredTime;
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
