//package com.pmsconnect.mage.user;
//
//import java.util.UUID;
//
//public class PmsConnection{
//    private String id;
//    private String userNamePms;
//    private String passwordPms;
//    private String pmsName;
//    private String processId;
//
//    public PmsConnection() {
//        this.id = UUID.randomUUID().toString();
//    }
//
//    public PmsConnection(String userNamePms, String passwordPms, String pmsName, String processId) {
//        this.id = UUID.randomUUID().toString();
//        this.userNamePms = userNamePms;
//        this.passwordPms = passwordPms;
//        this.pmsName = pmsName;
//        this.processId = processId;
//    }
//
//    public String getId() {
//        return id;
//    }
//
//    public String getUserNamePms() {
//        return userNamePms;
//    }
//
//    public void setUserNamePms(String userNamePms) {
//        this.userNamePms = userNamePms;
//    }
//
//    public String getPasswordPms() {
//        return passwordPms;
//    }
//
//    public void setPasswordPms(String passwordPms) {
//        this.passwordPms = passwordPms;
//    }
//
//    public String getPmsName() {
//        return pmsName;
//    }
//
//    public void setPmsName(String pmsName) {
//        this.pmsName = pmsName;
//    }
//
//    public String getProcessId() {
//        return processId;
//    }
//
//    public void setProcessId(String processId) {
//        this.processId = processId;
//    }
//
//    @Override
//    public String toString() {
//        return "PmsConnection{" +
//                "id='" + id + '\'' +
//                ", userNamePms='" + userNamePms + '\'' +
//                ", passwordPms='" + passwordPms + '\'' +
//                ", pmsName='" + pmsName + '\'' +
//                ", processId='" + processId + '\'' +
//                '}';
//    }
//}
