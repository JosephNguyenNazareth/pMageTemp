package com.pmsconnect.mage.utils;

public class Invitation {
    private String connectorId;
    private String invitor;
    private String invitee;
    private String inviteeUserName;

    public Invitation(String connectorId, String invitor, String invitee) {
        this.connectorId = connectorId;
        this.invitor = invitor;
        this.invitee = invitee;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getInvitor() {
        return invitor;
    }

    public void setInvitor(String invitor) {
        this.invitor = invitor;
    }

    public String getInvitee() {
        return invitee;
    }

    public void setInvitee(String invitee) {
        this.invitee = invitee;
    }

    public String getInviteeUserName() {
        return inviteeUserName;
    }

    public void setInviteeUserName(String inviteeUserName) {
        this.inviteeUserName = inviteeUserName;
    }

    @Override
    public String toString() {
        return "Invitation:" + invitor + ":" + connectorId + ":" + invitee;
    }
}
