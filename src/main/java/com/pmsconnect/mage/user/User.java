package com.pmsconnect.mage.user;

import com.pmsconnect.mage.utils.Invitation;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "user")
public class User {
    @Id
    private String userName;
    private String password;
    private List<String> listConnectorId;
    private String role;
    private List<Invitation> invitationSentList;
    private List<Invitation> invitationReceivedList;

    public User() {};

    public User(String userName, String password, List<String> listConnectorId, String role) {
        this.userName = userName;
        this.password = password;
        this.listConnectorId = listConnectorId;
        this.role = role;
        this.invitationSentList = new ArrayList<>();
        this.invitationReceivedList = new ArrayList<>();
    }

    public User(String userName, String password, String role) {
        this.userName = userName;
        this.password = password;
        this.role = role;
        this.listConnectorId = new ArrayList<>();
        this.invitationSentList = new ArrayList<>();
        this.invitationReceivedList = new ArrayList<>();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getListConnectorId() {
        return listConnectorId;
    }

    public void setListConnectorId(List<String> listConnectorId) {
        this.listConnectorId = listConnectorId;
    }

    public void addConnectorId(String connectorId) {
        this.listConnectorId.add(connectorId);
    }

    public void removeConnectorId(String connectorId) {
        this.listConnectorId.remove(connectorId);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<Invitation> getInvitationSentList() {
        return invitationSentList;
    }

    public void setInvitationSentList(List<Invitation> invitationSentList) {
        this.invitationSentList = invitationSentList;
    }

    public void addInvitationSentList(Invitation invitation) {
        this.invitationSentList.add(invitation);
    }

    public List<Invitation> getInvitationReceivedList() {
        return invitationReceivedList;
    }

    public void setInvitationReceivedList(List<Invitation> invitationReceivedList) {
        this.invitationReceivedList = invitationReceivedList;
    }

    public void addInvitationReceivedList(Invitation invitation) {
        this.invitationReceivedList.add(invitation);
    }
}
