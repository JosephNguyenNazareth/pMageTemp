package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.repo.UserRepo;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "connector")
public class Connector {
    @Id
    private String id;
    private String url;
    private String pmsProjectId;
    private UserRepo userRepo;
    private List<String> historyCommitList;
    private boolean isMonitoring;

    public Connector() {
    }

    public Connector(String url, String pmsProjectId, UserRepo userRepo) {
        this.id = UUID.randomUUID().toString();
        this.pmsProjectId = pmsProjectId;
        this.url = url;
        this.userRepo = userRepo;
        this.historyCommitList = new ArrayList<>();
        this.isMonitoring = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPmsProjectId() {
        return pmsProjectId;
    }

    public void setPmsProjectId(String pmsProjectId) {
        this.pmsProjectId = pmsProjectId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UserRepo getUserRepo() {
        return userRepo;
    }

    public void setUserRepo(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public List<String> getHistoryCommitList() {
        return historyCommitList;
    }

    public void setHistoryCommitList(List<String> historyCommitList) {
        this.historyCommitList = historyCommitList;
    }

    public void addHistoryCommitList(String historyCommit) {
        this.historyCommitList.add(historyCommit);
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    public void setMonitoring(boolean monitoring) {
        isMonitoring = monitoring;
    }
}
