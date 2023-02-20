package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.config.PmsConfig;
import com.pmsconnect.mage.config.Retriever;
import com.pmsconnect.mage.user.UserPMage;
import com.pmsconnect.mage.utils.Alignment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "connector")
public class Connector {
    @Id
    private String id;
    private UserPMage userPMage;
    private List<Alignment> historyCommitList;
    private Map<String, String> monitoringLog;
    private boolean isMonitoring;
    private Retriever retriever;
    private PmsConfig pmsConfig;

    public Connector() {
    }

    public Connector(UserPMage userPMage) {
        this.id = UUID.randomUUID().toString();
        this.userPMage = userPMage;
        this.historyCommitList = new ArrayList<>();
        this.monitoringLog = new HashMap<>();
        this.isMonitoring = false;
        this.retriever = new Retriever("./src/main/resources/repo_config.json");
        this.pmsConfig = new PmsConfig("./src/main/resources/pms_config.json", this.userPMage.getPmsName());
    }

    public String getId() {
        return id;
    }

    public List<Alignment> getHistoryCommitList() {
        return historyCommitList;
    }

    public void setHistoryCommitList(List<Alignment> historyCommitList) {
        this.historyCommitList = historyCommitList;
    }

    public void addHistoryCommitList(String historyCommit, String processInstanceChange, String commitTime, String changeTime, boolean isViolated) {
        this.historyCommitList.add(new Alignment(historyCommit, processInstanceChange, commitTime, changeTime, isViolated));
    }

    public void addHistoryCommitList(String historyCommit, String commitTime, boolean isViolated) {
        this.historyCommitList.add(new Alignment(historyCommit, "", commitTime, "", isViolated));
    }

    public void addHistoryCommitList(Alignment alignment) {
        this.historyCommitList.add(alignment);
    }

    public Alignment findCommitId(String commitId) {
        for (Alignment alignment: this.historyCommitList) {
            if (alignment.getCommitId().equals(commitId))
                return alignment;
        }
        return null;
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    public void setMonitoring(boolean monitoring) {
        isMonitoring = monitoring;
    }

    public Retriever getRetriever() {
        return retriever;
    }

    public void setRetriever(Retriever retriever) {
        this.retriever = retriever;
    }

    public PmsConfig getPmsConfig() {
        return pmsConfig;
    }

    public void setPmsConfig(PmsConfig pmsConfig) {
        this.pmsConfig = pmsConfig;
    }

    public UserPMage getUserPMage() {
        return userPMage;
    }

    public void setUserPMage(UserPMage userPMage) {
        this.userPMage = userPMage;
    }

    public Map<String, String> getMonitoringLog() {
        return monitoringLog;
    }

    public void addMonitoringLog(String moniotringMess) {
        this.monitoringLog.put(LocalDateTime.now().toString(), moniotringMess);
    }

    public void setMonitoringLog(Map<String, String> monitoringLog) {
        this.monitoringLog = monitoringLog;
    }
}
