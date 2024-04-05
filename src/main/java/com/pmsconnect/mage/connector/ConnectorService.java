package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.config.PMSConfigManager;
import com.pmsconnect.mage.config.PmsConfig;
import com.pmsconnect.mage.user.Bridge;
import com.pmsconnect.mage.user.PMSConnection;
import com.pmsconnect.mage.utils.ExternalService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

    public List<Connector> getConnectorsByUserName(String userName) {
        List<Connector> allConnectors = connectorRepository.findAll();
        List<Connector> userConnectors = new ArrayList<>();
        for (Connector connector: allConnectors) {
            if (connector.getUserName().equals(userName))
                userConnectors.add(connector);
        }
        return userConnectors;
    }

    public String addNewConnector(Bridge bridge) {
        if (!verifyPmsExist(bridge))
            throw new IllegalStateException("Cannot verify pms");
        Connector connector = new Connector(bridge);

        connectorRepository.save(connector);

        return connector.getId();
    }

    public String addSupplementaryConnectors(PMSConnection suppPMSConnection, String connectorId) {
        if (!verifyPmsExist(suppPMSConnection))
            throw new IllegalStateException("Cannot verify pms");

        Connector baseConnector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        Connector suppConnector = new Connector(new Bridge(baseConnector, suppPMSConnection));
        connectorRepository.save(suppConnector);

        return suppConnector.getId();
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

    private boolean verifyPmsExist(PMSConnection pmsConnection) {
        HttpClient client = HttpClients.createDefault();
        try {
            PmsConfig tmpConfig = new PmsConfig(pmsConnection.getPMSConfig(), pmsConnection.getPmsName());

            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", tmpConfig.getUrlPMS());
            urlMap.put("processInstanceId", pmsConnection.getProcessId());

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

    public List<String> extractKeywords(List<String> taskList) {
        StringBuilder taskListEntire = new StringBuilder();
        for (int i = 0; i < taskList.size(); i++) {
            taskListEntire.append("end task ");
            taskListEntire.append(taskList.get(i));
            if (i < taskList.size() - 1)
                taskListEntire.append(" | ");
        }

        File directory = new File("./src/main/python");
        List<String> commands = new ArrayList<>();
        commands.add("/home/nguyenminhkhoi/mambaforge/envs/fouille/bin/python");
        commands.add("keyword_extract.py");
        commands.add("\"" + taskListEntire + "\"");
        try {
            String keywords = ExternalService.runCommand(directory, commands);
            return Arrays.asList(keywords.split(";"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String generateActionEventTable(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + " does not exist."));

        // call get process instance from the pms
        // get the task list
        List<String> taskList = getTaskList(connector);

        // transfer the task list into NLP engine to get the keywords
        List<String> keywordList = extractKeywords(taskList);
        List<ActionEvent> listActionEvent = new ArrayList<>();
        for (int i = 0; i < taskList.size(); i++) {
            ActionEvent actionEvent = new ActionEvent("push-commit", keywordList.get(i), "endTask", taskList.get(i));
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


    public void loadHistoryCommit(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + " does not exist."));
        connector.getRetriever().setRepoLink(connector.getBridge().getProjectLink());
        List<Dictionary<String, String>> commitList = connector.getRetriever().getLatestCommitLog(true);


        for (Dictionary<String, String> commit : commitList) {
            String commitId = commit.get("id");
            String commitTime = commit.get("created_at");

            // if this commit is already in the history commit log of that connection
            if (connector.findCommitId(commitId) != null)
                continue;

            // skip validating the commit if the connector's owner is not the committer
            String committerName = commit.get("committer_name");
            if (!committerName.equals(connector.getBridge().getUserNameApp()))
                continue;

            connector.addHistoryCommitList(commitId, commitTime, false);
        }
        connectorRepository.save(connector);
    }

    public void loadProperties() {
        try {
            InputStream file = Connector.class.getResourceAsStream("/application.properties");
            if (file!=null) System.getProperties().load(file);
        } catch (IOException e) {
            throw new RuntimeException("Error loading application.properties", e);
        }
    }

    public List<String> getPMSConfig() {
        this.loadProperties();
        PMSConfigManager pmsManager = new PMSConfigManager(System.getProperty("pmsconfig"));
        return pmsManager.getListPMSName();
    }

    public String addPMSConfig(String pmsConfig) {
        this.loadProperties();
        PMSConfigManager pmsManager = new PMSConfigManager(System.getProperty("pmsconfig"));
        return pmsManager.addPMSConfig(pmsConfig);
    }
}
