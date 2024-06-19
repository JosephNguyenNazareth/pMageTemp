package com.pmsconnect.mage.utils;

public class PMSScore implements Comparable<PMSScore> {
    private String pmsName;
    private String userNamePms;
    private String passwordPms;
    private String pmsUrl;
    private int score;

    public PMSScore(String pmsName, String userNamePms, String passwordPms, String pmsUrl) {
        this.pmsName = pmsName;
        this.userNamePms = userNamePms;
        this.passwordPms = passwordPms;
        this.pmsUrl = pmsUrl;
        this.score = 1;
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
    public int compareTo(PMSScore other) {
        if (this.pmsName.equals(other.getPmsName())) {
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
        if (!(obj instanceof PMSScore))
            return false;
        PMSScore other = (PMSScore) obj;

        return this.pmsName.equals(other.getPmsName()) &&
                this.userNamePms.equals(other.getUserNamePms()) &&
                this.passwordPms.equals(other.getPasswordPms()) &&
                this.pmsUrl.equals(other.getPmsUrl());
    }
}
