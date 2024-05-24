package com.pmsconnect.mage.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "user")
public class User {
    @Id
    private String userName;
    private String password;
    private List<String> listConnectorId;
    private String role;


    public User() {};

    public User(String userName, String password, List<String> listConnectorId, String role) {
        this.userName = userName;
        this.password = password;
        this.listConnectorId = listConnectorId;
        this.role = role;
    }

    public User(String userName, String password, String role) {
        this.userName = userName;
        this.password = password;
        this.role = role;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
