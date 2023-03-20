package com.pmsconnect.mage.utils;

public class ActionEvent {
    private String action;
    private String event;
    private String eventDetail;
    private String task;

    public ActionEvent() {
    }

    public ActionEvent(String action, String event, String eventDetail, String task) {
        this.action = action;
        this.event = event;
        this.eventDetail = eventDetail;
        this.task = task;
    }

    public ActionEvent(String[] actionEventToken) {
        this.action = actionEventToken[0];
        this.event = actionEventToken[1];
        this.eventDetail = actionEventToken[2];
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

    public String getEventDetail() {
        return eventDetail;
    }

    public void setEventDetail(String eventDetail) {
        this.eventDetail = eventDetail;
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
                && otherActionEvent.getEventDetail().equals(this.getEventDetail())
                && otherActionEvent.getTask().equals(this.getTask());
    }
}
