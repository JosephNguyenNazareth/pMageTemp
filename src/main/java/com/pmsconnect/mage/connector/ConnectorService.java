package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.config.PmsConfig;
import com.pmsconnect.mage.user.Bridge;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.*;
import javax.transaction.Transactional;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import com.pmsconnect.mage.utils.ActionEvent;

@Service
public class ConnectorService {
    private final ConnectorRepository connectorRepository;

    @Autowired
    public ConnectorService(ConnectorRepository mageRepository) {
        this.connectorRepository = mageRepository;
    }

    public List<Connector> getConnectors() {
        return connectorRepository.findAll();
    }

    public Connector getConnector(String connectorId) {

        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        return connector;
    }

    public String addNewConnector(Bridge bridge) {
        if (!verifyPmsExist(bridge))
            throw new IllegalStateException("Cannot verify pms");
        Connector connector = new Connector(bridge);

        connectorRepository.save(connector);

        return connector.getId();
    }

    @Transactional
    public void updateConnector(String connectorId, Bridge bridge) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));
        connector.setBridge(bridge);

        connectorRepository.save(connector);
    }

    private boolean verifyPmsExist(Bridge bridge) {
        HttpClient client = HttpClients.createDefault();
        try {
            PmsConfig tmpConfig = new PmsConfig(bridge.getPMSConfig(), bridge.getPmsName());

            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", tmpConfig.getUrlPMS());
            urlMap.put("processInstanceId", bridge.getProcessId());

            String finalUri = tmpConfig.buildAPI("verify", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            return getStatusCode == 200;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getProcessInstance(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + " does not exist."));

        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());

            String finalUri = connector.getPmsConfig().buildAPI("verify", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200)
                return EntityUtils.toString(getResponse.getEntity());
            return "";
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public String createProcessInstance(String connectorId, String processName) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        if (!createPMSProcessInstance(connector, processName, connector.getBridge().getUserNamePms()))
            throw new IllegalStateException("Cannot create new process instance");
        connectorRepository.save(connector);

        return connector.getBridge().getProcessId();
    }

    private boolean createPMSProcessInstance(Connector connector, String processName, String creatorName) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            paramMap.put("processName", processName);
            paramMap.put("creatorName", creatorName);

            String finalUri = connector.getPmsConfig().buildAPI("createProject", urlMap, paramMap);
            HttpPost postMethod = new HttpPost(finalUri);
            HttpResponse getResponse = client.execute(postMethod);

            int postStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (postStatusCode != 200)
                return false;
            else {
                String responseBody = EntityUtils.toString(getResponse.getEntity());
                connector.getBridge().setProcessId(responseBody);
                return true;
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopMonitoringProcessInstance(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        connector.setMonitoring(false);
        connectorRepository.save(connector);
//        this.closeProcess(connector);
        System.out.println("Stop monitoring connector with id " + connectorId);
    }

    private void closeProcess(Connector connector) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());
            paramMap.put("processInstanceState", "true");

            String finalUri = connector.getPmsConfig().buildAPI("changeProcessState", urlMap, paramMap);
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot close process instance id " + connector.getBridge().getProcessId());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteConnector(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        connectorRepository.delete(connector);
    }

    public void addActionEventTable(String connectorId, String actionDescription) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));
        connector.setActionEventDescription(actionDescription);

        List<ActionEvent> updateActionEventList = new ArrayList<>();
        String[] actionEventList = actionDescription.split("\n");

        if (actionEventList.length == 1)
            if (actionEventList[0].equals(""))
                return;

        for (String actionEventStr : actionEventList) {
            String[] actionEventToken = actionEventStr.split(",");
            ActionEvent actionEvent = new ActionEvent(actionEventToken);
            if (!updateActionEventList.contains(actionEvent))
                updateActionEventList.add(actionEvent);
        }

        connector.setActionEventTable(updateActionEventList);
        connectorRepository.save(connector);
    }

    public List<String> getTaskList(Connector connector ) {
        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());

            String finalUri = connector.getPmsConfig().buildAPI("getTask", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());

                if (connector.getBridge().getPmsName().equals("core-bape")) {
                    content = content.replace("[","").replace("]","").replace("\"","");
                    return Arrays.asList(content.split(","));
                }
//                JSONParser parser = new JSONParser();
//                Object obj = parser.parse(content);
//                JSONArray contentJSON = (JSONArray) obj;
//                for (JSONObject)
            }
//                return EntityUtils.toString(getResponse.getEntity());
            return null;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateActionEventTable(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + " does not exist."));

        // call get process instance from the pms
        // get the task list
        List<String> taskList = getTaskList(connector);
        System.out.println(taskList);

        // transfer the task list into NLP engine to get the keywords
        List<ActionEvent> listActionEvent = new ArrayList<>();
        for (String taskName : taskList) {
            // example by taking substring!!!
            // TODO: connect to a NLP service to get the keywords
            String suggestedKeywords = taskName.substring(2);
            ActionEvent actionEvent = new ActionEvent("push-commit", suggestedKeywords, "endTask", taskName);
            listActionEvent.add(actionEvent);
        }

        // generate the action linkage table
        StringBuilder actionLinkage = new StringBuilder();
        for (int i = 0; i < listActionEvent.size(); i++) {
            actionLinkage.append(listActionEvent.get(i).toString());
            if (i < listActionEvent.size() - 1)
                actionLinkage.append("\n");
        }

        return actionLinkage.toString();
    }
}
