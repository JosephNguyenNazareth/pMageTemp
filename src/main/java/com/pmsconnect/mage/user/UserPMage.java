package com.pmsconnect.mage.user;

public class UserPMage {
    private String userName;
    private String personalToken;
    private String originRepo;
    private String repoRemote;
    private String repoLocal;
    private String realName;
    private String password;
    private String pmsName;
    private String projectId;

    public UserPMage() {
    }

    public UserPMage(String userName, String personalToken, String originRepo, String repoRemote, String repoLocal, String realName, String password, String pmsName, String projectId) {
        this.userName = userName;
        this.personalToken = personalToken;
        this.originRepo = originRepo;
        this.repoRemote = repoRemote;
        this.repoLocal = repoLocal;
        this.realName = realName;
        this.password = password;
        this.pmsName = pmsName;
        this.projectId = projectId;
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

    public String getRepoRemote() {
        return repoRemote;
    }

    public void setRepoRemote(String repoRemote) {
        this.repoRemote = repoRemote;
    }

    public String getRepoLocal() {
        return repoLocal;
    }

    public void setRepoLocal(String repoLocal) {
        this.repoLocal = repoLocal;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPmsName() {
        return pmsName;
    }

    public void setPmsName(String pmsName) {
        this.pmsName = pmsName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UserPMage))
            return false;
        UserPMage otherUser = (UserPMage) other;
        return this.userName.equals(otherUser.getUserName())
                && this.password.equals(otherUser.getPassword())
                && this.pmsName.equals(otherUser.getPmsName())
                && this.projectId.equals(otherUser.getProjectId())
                && this.realName.equals(otherUser.getRealName())
                && this.personalToken.equals(otherUser.getPersonalToken())
                && this.originRepo.equals(otherUser.getOriginRepo())
                && this.repoRemote.equals(otherUser.getRepoRemote())
                && this.repoLocal.equals(otherUser.getRepoLocal());
    }
}
