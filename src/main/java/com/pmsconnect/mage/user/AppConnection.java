//package com.pmsconnect.mage.user;
//
//import java.util.UUID;
//
//public class AppConnection {
//    private String id;
//
//
//    public AppConnection() {
//        this.id = UUID.randomUUID().toString();
//    }
//
//    public AppConnection(String appName, String userNameApp, String passwordApp, String projectLink, String projectDir) {
//        this.id = UUID.randomUUID().toString();
//        this.appName = appName;
//        this.userNameApp = userNameApp;
//        this.passwordApp = passwordApp;
//        this.projectLink = projectLink;
//        this.projectDir = projectDir;
//    }
//
//    public String getId() {
//        return id;
//    }
//
//    public String getAppName() {
//        return appName;
//    }
//
//    public void setAppName(String appName) {
//        this.appName = appName;
//    }
//
//    public String getUserNameApp() {
//        return userNameApp;
//    }
//
//    public void setUserNameApp(String userNameApp) {
//        this.userNameApp = userNameApp;
//    }
//
//    public String getPasswordApp() {
//        return passwordApp;
//    }
//
//    public void setPasswordApp(String passwordApp) {
//        this.passwordApp = passwordApp;
//    }
//
//    public String getProjectLink() {
//        return projectLink;
//    }
//
//    public void setProjectLink(String projectLink) {
//        this.projectLink = projectLink;
//    }
//
//    public String getProjectDir() {
//        return projectDir;
//    }
//
//    public void setProjectDir(String projectDir) {
//        this.projectDir = projectDir;
//    }
//
//    @Override
//    public String toString() {
//        return "AppConnection{" +
//                "id='" + id + '\'' +
//                ", appName='" + appName + '\'' +
//                ", userNameApp='" + userNameApp + '\'' +
//                ", passwordApp='" + passwordApp + '\'' +
//                ", projectLink='" + projectLink + '\'' +
//                ", projectDir='" + projectDir + '\'' +
//                '}';
//    }
//}
