package com.pmsconnect.mage.utils;

public class AppScore implements Comparable<AppScore> {
    private String appName;
    private String userNameApp;
    private String passwordApp;
    private String projectLink;
    private String projectDir;
    private int score;

    public AppScore(String appName, String userNameApp, String passwordApp, String projectLink, String projectDir) {
        this.appName = appName;
        this.userNameApp = userNameApp;
        this.passwordApp = passwordApp;
        this.projectLink = projectLink;
        this.projectDir = projectDir;
        this.score = 1;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUserNameApp() {
        return userNameApp;
    }

    public void setUserNameApp(String userNameApp) {
        this.userNameApp = userNameApp;
    }

    public String getPasswordApp() {
        return passwordApp;
    }

    public void setPasswordApp(String passwordApp) {
        this.passwordApp = passwordApp;
    }

    public String getProjectLink() {
        return projectLink;
    }

    public void setProjectLink(String projectLink) {
        this.projectLink = projectLink;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void increase() {
        this.score += 1;
    }

    @Override
    public int compareTo(AppScore other) {
        if (this.appName.equals(other.getAppName())) {
            if (this.score == other.getScore())
                return 0;
            if (this.score < other.getScore())
                return -1;
            if (this.score > other.getScore())
                return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AppScore))
            return false;
        AppScore other = (AppScore) obj;

        return this.appName.equals(other.getAppName()) &&
                this.userNameApp.equals(other.getUserNameApp()) &&
                this.passwordApp.equals(other.getPasswordApp()) &&
                this.projectLink.equals(other.getProjectLink()) &&
                this.projectLink.equals(other.getProjectDir());
    }
}
