package com.pmsconnect.mage.utils;

public class Alignment {
    private String commitId;
    private String processInstanceChange;
    private String  commitTime;
    private String processInstanceChangeTime;
    private Boolean isViolated;

    public Alignment() {
    }

    public Alignment(String commitId, String processInstanceChange, String commitTime, String processInstanceChangeTime, Boolean isViolated) {
        this.commitId = commitId;
        this.processInstanceChange = processInstanceChange;
        this.commitTime = commitTime;
        this.processInstanceChangeTime = processInstanceChangeTime;
        this.isViolated = isViolated;
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
        return isViolated;
    }

    public void setViolated(Boolean violated) {
        isViolated = violated;
    }
}
