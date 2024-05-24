package com.pmsconnect.mage.user;

import com.pmsconnect.mage.connector.Connector;

import java.io.IOException;
import java.io.InputStream;

public class PMSConnection {
    private String pmsName;
    private String userNamePms;
    private String passwordPms;
    private String pmsUrl;
    private String processId;
    private String userName;

    public PMSConnection(String userNamePms, String passwordPms, String pmsName, String pmsUrl, String processId) {
        this.userNamePms = userNamePms;
        this.passwordPms = passwordPms;
        this.pmsName = pmsName;
        this.pmsUrl = pmsUrl;
        this.processId = processId;
        this.loadProperties();
    }

    public String getPmsName() {
        return pmsName;
    }

    public void setPmsName(String pmsName) {
        this.pmsName = pmsName;
    }

    public String getUserNamePms() {
        return userNamePms;
    }

    public void setUserNamePms(String userNamePms) {
        this.userNamePms = userNamePms;
    }

    public String getPasswordPms() {
        return passwordPms;
    }

    public void setPasswordPms(String passwordPms) {
        this.passwordPms = passwordPms;
    }

    public String getPmsUrl() {
        return pmsUrl;
    }

    public void setPmsUrl(String pmsUrl) {
        this.pmsUrl = pmsUrl;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void loadProperties() {
        try {
            InputStream file = Connector.class.getResourceAsStream("/application.properties");
            if (file!=null) System.getProperties().load