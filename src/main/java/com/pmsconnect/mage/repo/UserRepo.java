package com.pmsconnect.mage.repo;

import java.util.HashMap;
import java.util.Map;

public class UserRepo {
    private String realName;
    private String userName;
    private String personalToken;
    private Repo originRepo;
    private Map<String, String> repoMap;

    public UserRepo(String userName, String userRealName, String personalToken, Repo originRepo) {
        this.userName = userName;
        this.realName = userRealName;
        this.personalToken = personalToken;
        this.originRepo = originRepo;
        this.repoMap = new HashMap<>();
    }

    public String getRealName() {
        return realName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPersonalToken() {
        return personalToken;
    }

    public void setPersonalToken(String personalToken) {
        this.personalToken = personalToken;
    }

    public Repo getOriginRepo() {
        return originRepo;
    }

    public void setOriginRepo(Repo originRepo) {
        this.originRepo = originRepo;
    }

    public Map<String, String> getRepoMap() {
        return repoMap;
    }

    public void setRepoMap(Map<String, String> repoMap) {
        this.repoMap = repoMap;
    }

    public void addRepoMap(String repoLink, String repoDir) {
        this.repoMap.put(repoLink, repoDir);
    }
}
