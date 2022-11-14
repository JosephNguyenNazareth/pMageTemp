package com.pmsconnect.mage.repo;

import java.util.HashMap;
import java.util.Map;

public class UserRepo {
    private String realName;
    private String userName;
    private String personalToken;
    private String originRepo;
    private String repoLink;
    private String repoDir;

    public UserRepo() {
    }

    public UserRepo(String realName, String userName, String personalToken, String originRepo, String repoLink, String repoDir) {
        this.realName = realName;
        this.userName = userName;
        this.personalToken = personalToken;
        this.originRepo = originRepo;
        this.repoLink = repoLink;
        this.repoDir = repoDir;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
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

    public String getOriginRepo() {
        return originRepo;
    }

    public void setOriginRepo(String originRepo) {
        this.originRepo = originRepo;
    }

    public String getRepoLink() {
        return repoLink;
    }

    public void setRepoLink(String repoLink) {
        this.repoLink = repoLink;
    }

    public String getRepoDir() {
        return repoDir;
    }

    public void setRepoDir(String repoDir) {
        this.repoDir = repoDir;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UserRepo))
            return false;
        UserRepo otherUser = (UserRepo) other;
        return this.realName.equals(((UserRepo) other).getRealName()) && this.userName.equals(otherUser.getUserName()) && this.originRepo.equals(otherUser.getOriginRepo());
    }
}
