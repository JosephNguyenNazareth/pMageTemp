//package com.pmsconnect.mage.user;
//
//import org.springframework.data.annotation.Id;
//import org.springframework.data.mongodb.core.mapping.Document;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Document(collection = "user")
//public class UserPMage {
//    @Id
//    private String id;
//    private String userName;
//    private String userEmail;
//    private List<String> connectorIdList;
//
//    public UserPMage() {
//    }
//
//    public UserPMage(String userName, String userEmail) {
//        this.id = UUID.randomUUID().toString();
//        this.userName = userName;
//        this.userEmail = userEmail;
//        this.connectorIdList = new ArrayList<>();
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public void setUserName(String userName) {
//        this.userName = userName;
//    }
//
//    public String getUserEmail() {
//        return userEmail;
//    }
//
//    public void setUserEmail(String userEmail) {
//        this.userEmail = userEmail;
//    }
//
//    public List<String> getConnectorIdList() {
//        return connectorIdList;
//    }
//
//    public void addConnectorId(String connectorId) {
//        this.connectorIdList.add(connectorId);
//    }
//
//    @Override
//    public String toString() {
//        return "UserPMage{" +
//                "userName='" + userName + '\'' +
//                ", userEmail='" + userEmail + '\'' +
//                ", connectorIdList=" + connectorIdList +
//                '}';
//    }
//}
