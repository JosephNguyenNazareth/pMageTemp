package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.config.PMSConfigManager;
import com.pmsconnect.mage.config.PmsConfig;
import com.pmsconnect.mage.user.Bridge;
import com.pmsconnect.mage.user.User;
import com.pmsconnect.mage.user.UserRepository;
import com.pmsconnect.mage.utils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.*;

@Service
public class ConnectorService {
    private final ConnectorRepository connectorRepository;
    private final SuppConnectorRepository suppConnectorRepository;
    private final UserRepository userRepository;

    @Autowired
    public ConnectorService(ConnectorRepository mageRepository, SuppConnectorRepository mageRepository2, UserRepository userRepository) {
        this.connectorRepository = mageRepository;
        this.suppConnectorRepository = mageRepository2;
        this.userRepository = userRepository;
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
            if (connector.getUserName().equals(userName)) {
                userConnectors.add(connector);

                // update user list connectorId in case it does not have it yet
                User user = userRepository.findById(userName).orElseThrow(() -> new IllegalStateException("User with userName " + userName + "does not exist."));
                if (user.getListConnectorId() == null) {
                    List<String> connectorList = new ArrayList<>();
                    connectorList.add(connector.getId());
                    user.setListConnectorId(connectorList);
                } else {
                    if (!user.getListConnectorId().contains(connector.getId()))
                        user.addConnectorId(connector.getId());
                }
                userRepository.save(user);
            }

        }
        return userConnectors;
    }

    public String addNewConnector(Bridge bridge, boolean inviteCollab) {
        if (!verifyPmsExist(bridge))
            throw new IllegalStateException("Cannot verify pms");
        Connector connector = new Connector(bridge);
        updateArtifactList(connector);

        User userPMage = userRepository.findById(connector.getUserName()).orElseThrow(() -> new IllegalStateException("User with username " + connector.getUserName() + "does not exist."));
        userPMage.addConnectorId(connector.getId());
        userRepository.save(userPMage);

        if (inviteCollab)
            if (userPMage.getRole().equals("manager")) {
                List<String> collaborators = this.getProcessActors(bridge);

            }

        connectorRepository.save(connector);

        return connector.getId();
    }

    public String addSupplementaryConnectors(Bridge bridge, String connectorId) {
        if (!verifyPmsExist(bridge))
            throw new IllegalStateException("Cannot verify pms");

        Connector baseConnector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        SupplementaryConnector suppConnector = new SupplementaryConnector(bridge, connectorId,
                baseConnector.getBridge().getAppName(),
                baseConnector.getBridge().getProjectDir(), baseConnector.getBridge().getProjectLink());
        updateArtifactList(suppConnector);

        User userPMage = userRepository.findById(suppConnector.getUserName()).orElseThrow(() -> new IllegalStateException("User with username " + suppConnector.getUserName() + "does not exist."));
        userPMage.addConnectorId(suppConnector.getId());
        userRepository.save(userPMage);

        assert suppConnectorRepository != null;
        suppConnectorRepository.save(suppConnector);

        return suppConnector.getId();
    }

    public String addSupplementaryConnectorsPast(Bridge bridge, String artifactList) {
        if (!verifyPmsExist(bridge))
            throw new IllegalStateException("Cannot verify pms");

        SupplementaryConnector suppConnector = new SupplementaryConnector(bridge, artifactList);
        updateArtifactList(suppConnector);

        User userPMage = userRepository.findById(suppConnector.getUserName()).orElseThrow(() -> new IllegalStateException("User with username " + suppConnector.getUserName() + "does not exist."));
        userPMage.addConnectorId(suppConnector.getId());
        userRepository.save(userPMage);

        assert suppConnectorRepository != null;
        suppConnectorRepository.save(suppConnector);

        return suppConnector.getId();
    }

    @Transactional
    public void updateConnector(String connectorId, Bridge bridge, String taskArtifact) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));
        connector.setBridge(bridge);

        connectorRepository.save(connector);
    }

    @Transactional
    public void updateSuppConnector(String connectorId, Bridge bridge, String baseConnectorId, String taskArtifact) {
        Connector baseConnector = connectorRepository.findById(baseConnectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        SupplementaryConnector connector = suppConnectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        connector.setBridge(bridge);
        connector.setSuppAppName(baseConnector.getBridge().getAppName());
        connector.setSuppProjectDir(baseConnector.getBridge().getProjectDir());
        connector.setSuppProjectLink(baseConnector.getBridge().getProjectLink());

        suppConnectorRepository.save(connector);
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

            if (bridge.getPmsName().equals("bonita")) {
                String loginInfo = this.loginPMS(bridge.getPmsName());
                String[] loginInfoList = loginInfo.split(";");
                getMethod.setHeader("X-Bonita-API-Token", loginInfoList[1]);
                getMethod.setHeader("Cookie", "JSESSIONID=" + loginInfoList[0]);
            }

            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine().getStatusCode();
            return getStatusCode == 200;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getProcessActors(Bridge bridge) {
        HttpClient client = HttpClients.createDefault();
        try {
            PmsConfig tmpConfig = new PmsConfig(bridge.getPMSConfig(), bridge.getPmsName());

            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", tmpConfig.getUrlPMS());
            urlMap.put("processInstanceId", bridge.getProcessId());

            String finalUri = tmpConfig.buildAPI("getActors", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);

//            if (bridge.getPmsName().equals("bonita")) {
//                String loginInfo = this.loginPMS(bridge.getPmsName());
//                String[] loginInfoList = loginInfo.split(";");
//                getMethod.setHeader("X-Bonita-API-Token", loginInfoList[1]);
//                getMethod.setHeader("Cookie", "JSESSIONID=" + loginInfoList[0]);
//            }

            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine().getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());
                if (bridge.getPmsName().equals("core-bape")) {
                    content = content.replace("[", "").replace("]", "").replace("\"", "");
                    return Arrays.asList(content.split(","));
                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }



//    private void fillEssentialInfo(HttpGet request, Map<String, Map<String, String>> extraInfo) {
//        for (Map.Entry<String,Map<String, String>> entry : extraInfo.entrySet()) {
//            if (entry.getKey().equals("header")) {
//                Map<String, String> headerSet = entry.getValue();
//                for (Map.Entry<String, String> headerEntry : headerSet.entrySet())
//                    request.setHeader(headerEntry.getKey(), headerEntry.getValue());
//            } else if (entry.getKey().equals("cookie")) {
//            } else if (entry.getKey().equals("body")) {
//
//            }
//        }
//    }
//
//    private Map<String, Map<String, String>> searchEssentialInfo(PMSConnection pmsConnection, PmsConfig pmsConfig, String functionName) {
//        Map<String, String> headerSet = pmsConfig.provideExtraInfo("header", functionName);
//        Map<String, String> cookieSet = pmsConfig.provideExtraInfo("cookie", functionName);
//        Map<String, String> bodySet = pmsConfig.provideExtraInfo("body", functionName);
//
//        Map<String, Map<String, String>> finalSet = new HashMap<>();
//        finalSet.put("header", headerSet);
//        finalSet.put("cookie", cookieSet);
//        finalSet.put("body", bodySet);
//
//        return loadEssentialInfo(pmsConnection, finalSet);
//    }
//
//    private Map<String, Map<String, String>> loadEssentialInfo(PMSConnection pmsConnection, Map<String, Map<String, String>> extraInfoSet) {
//        Map<String, Map<String, String>> extraInfoSetCompleted = new HashMap<>(extraInfoSet);
//        Map<String, Map<String, String>> requestedInfo = new HashMap<>();
//
//        for (Map.Entry<String,Map<String, String>> entryField : extraInfoSet.entrySet()) {
//            for (Map.Entry<String, String> entry : entryField.getValue().entrySet()) {
//                String key = entry.getKey();
//                String value = entry.getValue();
//                if (value.contains(":")) {
//                    String[] valueDetail = value.split(":");
//                    if (!requestedInfo.containsKey(valueDetail[0])) {
//                        Map<String, String> fields = new HashMap<>();
//                        fields.put(key, valueDetail[1]);
//                        requestedInfo.put(valueDetail[0], fields);
//                    } else {
//                        requestedInfo.get(valueDetail[0]).put(key, valueDetail[1]);
//                    }
//                }
//            }
//        }
//
//        for (Map.Entry<String,Map<String, String>> entry : requestedInfo.entrySet()) {
//            String key = entry.getKey();
//            if (key.equals("login")) {
//                Map<String, String> returnedMap= loginPMS(pmsConnection, entry.getValue());
//                for (String returnedKey: returnedMap.keySet()) {
//                    for (Map.Entry<String,Map<String, String>> entryCompleted : extraInfoSetCompleted.entrySet())
//                        if (entryCompleted.getValue().containsKey(returnedKey))
//                            extraInfoSetCompleted.get(entryCompleted.getKey()).put(returnedKey, returnedMap.get(returnedKey));
//                }
//            }
//        }
//
//        return extraInfoSetCompleted;
//    }

    public String loginPMS(String pmsName) {
//        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        HttpClient client = HttpClients.createDefault();
        try {
            PmsConfig tmpConfig = new PmsConfig("./src/main/resources/pms_config.json", pmsName);

            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();
            urlMap.put("url", tmpConfig.getUrlPMS());

            List<NameValuePair> pairs = new ArrayList<>();
            pairs.add(new BasicNameValuePair("username", "walter.bates"));
            pairs.add(new BasicNameValuePair("password", "bpm"));

            String finalUri = tmpConfig.buildAPI("login", urlMap, paramMap);
            HttpPost postMethod = new HttpPost(finalUri);
            postMethod.setHeader("Content-type", "application/x-www-form-urlencoded");
            postMethod.setEntity(new UrlEncodedFormEntity(pairs));

            HttpResponse response = client.execute(postMethod);
            String jSessionId = "";
            String bonitaToken = "";

            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                if (header.getName().equals("Set-Cookie")) {
                    String headerValue = header.getValue();
                    if (headerValue.contains("JSESSIONID")) {
                        jSessionId = headerValue.substring(headerValue.indexOf("JSESSIONID") + 11, headerValue.indexOf(";", headerValue.indexOf("JSESSIONID")));
                    } else if (headerValue.contains("X-Bonita-API-Token")) {
                        bonitaToken = headerValue.substring(headerValue.indexOf("X-Bonita-API-Token") + 19, headerValue.indexOf(";", headerValue.indexOf("X-Bonita-API-Token")));
                    }
                }
            }

            System.out.println(jSessionId);
            System.out.println(bonitaToken);

            int statusCode = response.getStatusLine()
                    .getStatusCode();
            return jSessionId + ";" + bonitaToken;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getProcessInstanceIdList(String pmsName, String pmsURL, String usernamePMS, String passwordPMS, String processDef) {
        HttpClient client = HttpClients.createDefault();
        PmsConfig pmsConfig = new PmsConfig(System.getProperty("pmsconfig"), pmsName);
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", pmsURL);
            urlMap.put("processDef", processDef);

            String finalUri = pmsConfig.buildAPI("getCase", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);

            // for early development only
            // TODO: make it generic for all other PMSs
            if (pmsName.equals("bonita")) {
                String loginInfo = this.loginPMS(pmsName);
                String[] loginInfoList = loginInfo.split(";");
                getMethod.setHeader("X-Bonita-API-Token", loginInfoList[1]);
                getMethod.setHeader("Cookie", "JSESSIONID=" + loginInfoList[0]);
            }

            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());

                if (pmsName.equals("core-bape")) {
                    content = content.replace("[","").replace("]","").replace("\"","");
                    return Arrays.asList(content.split(","));
                } else if (pmsName.equals("bonita")) {
                    JSONParser parser = new JSONParser();
                    JSONArray caseList = (JSONArray) parser.parse(content);

                    List<String> caseIdList = new ArrayList<>();
                    for (Object o : caseList) {
                        JSONObject task = (JSONObject) o;
                        String caseId = task.get("id").toString();
                        caseIdList.add(caseId);
                    }
                    return caseIdList;
                }
//                JSONParser parser = new JSONParser();
//                Object obj = parser.parse(content);
//                JSONArray contentJSON = (JSONArray) obj;
//                for (JSONObject)
            }
//                return EntityUtils.toString(getResponse.getEntity());
            return null;
        } catch (URISyntaxException | IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }


//    private Map<String, String> loginPMS(PMSConnection pmsConnection, Map<String, String> extractInfo) {
//        HttpClient client = HttpClients.createDefault();
//        try {
//            PmsConfig tmpConfig = new PmsConfig(pmsConnection.getPMSConfig(), pmsConnection.getPmsName());
//
//            Map<String, String> urlMap = new HashMap<>();
//            Map<String, String> paramMap = new HashMap<>();
//
//            urlMap.put("url", tmpConfig.getUrlPMS());
//
//            String finalUri = tmpConfig.buildAPI("login", urlMap, paramMap);
//            HttpPost postMethod = new HttpPost(finalUri);
//            HttpResponse getResponse = client.execute(postMethod);
//
//            int getStatusCode = getResponse.getStatusLine()
//                    .getStatusCode();
//
//        } catch (URISyntaxException | IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        return null;
//    }

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

        // check role of user
        User userPMage = userRepository.findById(connector.getUserName()).orElseThrow(() -> new IllegalStateException("User with username " + connector.getUserName() + "does not exist."));
        if (!userPMage.getRole().equals("process-owner"))
            throw new IllegalStateException("Error. Unsatisfied privilege. Cannot create new process instance");

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

        User userPMage = userRepository.findById(connector.getUserName()).orElseThrow(() -> new IllegalStateException("User with username " + connector.getUserName() + "does not exist."));
        userPMage.removeConnectorId(connector.getId());
        userRepository.save(userPMage);

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

            // for early development only
            // TODO: make it generic for all other PMSs
            if (connector.getBridge().getPmsName().equals("bonita")) {
                String loginInfo = this.loginPMS(connector.getBridge().getPmsName());
                String[] loginInfoList = loginInfo.split(";");
                getMethod.setHeader("X-Bonita-API-Token", loginInfoList[1]);
                getMethod.setHeader("Cookie", "JSESSIONID=" + loginInfoList[0]);
            }

            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());

                if (connector.getBridge().getPmsName().equals("core-bape")) {
                    content = content.replace("[","").replace("]","").replace("\"","");
                    return Arrays.asList(content.split(","));
                } else if (connector.getBridge().getPmsName().equals("bonita")) {
                    JSONParser parser = new JSONParser();
                    JSONArray taskList = (JSONArray) parser.parse(content);

                    List<String> taskNameList = new ArrayList<>();
                    for (Object o : taskList) {
                        JSONObject task = (JSONObject) o;
                        String taskName = task.get("name").toString();
                        taskNameList.add(taskName);
                    }
                    return taskNameList;
                }
//                JSONParser parser = new JSONParser();
//                Object obj = parser.parse(content);
//                JSONArray contentJSON = (JSONArray) obj;
//                for (JSONObject)
            }
//                return EntityUtils.toString(getResponse.getEntity());
            return null;
        } catch (URISyntaxException | IOException | ParseException e) {
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
        connector.updateConfig();
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

        connectorRepository.save(connector);
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

    public List<Alignment> getConnectorHist(String baseConnectorId) {
        Connector baseConnector = connectorRepository.findById(baseConnectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + baseConnectorId + " does not exist."));

        return baseConnector.getHistoryCommitList();
    }

    public void updateArtifactList(Connector connector) {
        if (connector.getBridge().getPmsName().equals("bonita")) {
            for (TaskArtifact taskArtifact: connector.getTaskArtifactList()) {
                for (Artifact input: taskArtifact.getInput()) {
                    if (!connector.getArtifactPool().containsKey(input.getName())) {
                        connector.addArtifact(input);
                    }
                }

                for (Artifact output: taskArtifact.getOutput()) {
                    if (!connector.getArtifactPool().containsKey(output.getName())) {
                        connector.addArtifact(output);
                    }
                }
            }
        }

        HttpClient client = HttpClients.createDefault();
        try {
            Map<String, String> urlMap = new HashMap<>();
            Map<String, String> paramMap = new HashMap<>();

            urlMap.put("url", connector.getPmsConfig().getUrlPMS());
            urlMap.put("processInstanceId", connector.getBridge().getProcessId());

            String finalUri = connector.getPmsConfig().buildAPI("getArtifact", urlMap, paramMap);
            HttpGet getMethod = new HttpGet(finalUri);

            // for early development only
            // TODO: make it generic for all other PMSs
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200) {
                String content = EntityUtils.toString(getResponse.getEntity());

                if (connector.getBridge().getPmsName().equals("core-bape")) {
                    JSONParser parser = new JSONParser();
                    JSONArray taskList = (JSONArray) parser.parse(content);

                    for (Object o : taskList) {
                        JSONObject task = (JSONObject) o;
                        String artifactName = task.get("name").toString();
                        connector.addArtifact(artifactName);
                    }
                }
            }
        } catch (URISyntaxException | IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
