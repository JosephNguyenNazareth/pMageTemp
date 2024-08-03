package com.pmsconnect.mage.user;

import com.pmsconnect.mage.connector.Connector;
import com.pmsconnect.mage.connector.ConnectorRepository;
import com.pmsconnect.mage.utils.AppScore;
import com.pmsconnect.mage.utils.PMSScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {
    @Autowired
    private final UserRepository userRepository;
    private final ConnectorRepository connectorRepository;

    public UserService(UserRepository userRepository, ConnectorRepository connectorRepository) {
        this.userRepository = userRepository;
        this.connectorRepository = connectorRepository;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User getUser(String userName) {
        return userRepository.findById(userName).orElseThrow(() -> new IllegalStateException("User with userName " + userName + "does not exist."));
    }

    public User login(String userName, String password) {
        User user = userRepository.findById(userName).orElseThrow(() -> new IllegalStateException("User with userName " + userName + "does not exist."));
        if(user.getPassword().equals(password))
            return user;
        return null;
    }

    public String addNewUser(String userName, String password, String role) {
        if (userExist(userName))
            return "User with username " + userName + " already exists. Please choose another username.";

        User user = new User(userName, password, role);
        userRepository.save(user);
        return "Successfully created new user " + userName;
    }

    public boolean userExist(String userName) {
        return userRepository.existsById(userName);
    }

    public List<PMSScore> getUserCommonPMSInfo(String userName, String selectedApp) {
        List<PMSScore> infoConfidence = new ArrayList<>();
        User user = userRepository.findById(userName).orElseThrow(() -> new IllegalStateException("User with userName " + userName + "does not exist."));
        for (String connectorId: user.getListConnectorId()) {
            Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));
            if (connector.getBridge().getPmsName().equals(selectedApp)) {
                PMSScore appScore = new PMSScore(selectedApp,
                                                connector.getBridge().getUserNamePms(),
                                                connector.getBridge().getPasswordPms(),
                                                connector.getBridge().getPmsUrl());
                if (!infoConfidence.contains(appScore))
                    infoConfidence.add(appScore);
                else
                    infoConfidence.get(infoConfidence.indexOf(appScore)).increase();
            }
        }
        infoConfidence.sort(Collections.reverseOrder());
        System.out.println(infoConfidence);

        return infoConfidence;
    }

    public List<AppScore> getUserCommonAppInfo(String userName, String selectedApp) {
        List<AppScore> infoConfidence = new ArrayList<>();
        User user = userRepository.findById(userName).orElseThrow(() -> new IllegalStateException("User with userName " + userName + "does not exist."));

        for (String connectorId: user.getListConnectorId()) {
            Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));
            if (connector.getBridge().getAppName().equals(selectedApp)) {
                AppScore appScore = new AppScore(selectedApp,
                        connector.getBridge().getUserNameApp(),
                        connector.getBridge().getPasswordApp(),
                        connector.getBridge().getProjectLink(),
                        connector.getBridge().getProjectDir());
                if (!infoConfidence.contains(appScore))
                    infoConfidence.add(appScore);
                else
                    infoConfidence.get(infoConfidence.indexOf(appScore)).increase();
            }
        }
        infoConfidence.sort(Collections.reverseOrder());
        System.out.println(infoConfidence);

        return infoConfidence;
    }
}
