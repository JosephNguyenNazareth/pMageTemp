package com.pmsconnect.mage.utils;

public class ActionEvent {
    private String action;
    private String event;
    private String actionDetail;
    private String task;

    public ActionEvent() {
    }

    public ActionEvent(String action, String actionDetail, String event, String task) {
        this.action = action;
        this.actionDetail = actionDetail;
        this.event = event;
        this.task = task;
    }

    public ActionEvent(String[] actionEventToken) {
        this.action = actionEventToken[0];
        this.actionDetail = actionEventToken[1];
        this.event = actionEventToken[2];
        this.task = actionEventToken[3];
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getActionDetail() {
        return actionDetail;
    }

    public void setActionDetail(String actionDetail) {
        this.actionDetail = actionDetail;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ActionEvent))
            return false;
        if (other == this)
            return true;
        ActionEvent otherActionEvent = (ActionEvent) other;
        return otherActionEvent.getAction().equals(this.getAction())
                && otherActionEvent.getEvent().equals(this.getEvent())
                && otherActionEvent.getActionDetail().equals(this.getActionDetail())
                && otherActionEvent.getTask().equals(this.getTask());
    }
}
