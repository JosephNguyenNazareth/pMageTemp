package com.pmsconnect.mage.config;

import com.pmsconnect.mage.user.Bridge;
import com.pmsconnect.mage.utils.ActionEvent;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AppConfig {
    private String projectLink;
    private JSONObject config;
    private String configPath;
    private String app;

    public AppConfig() {

    }

    public AppConfig(String configPath) {
        this.configPath = configPath;
        this.readConfig();
    }

    public AppConfig(String configPath, String projectLink, JSONObject config) {
        this.projectLink = projectLink;
        this.config = config;
        this.configPath = configPath;
        this.readConfig();
    }

    public String getProjectLink() {
        return projectLink;
    }

    public JSONObject getConfig() {
        return config;
    }

    public void setConfig(JSONObject config) {
        this.config = config;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void setProjectLink(String projectLink) {
        this.projectLink = projectLink;
    }

    public void readConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(this.configPath)));
            JSONArray configList = new JSONArray(content);
            for (int i = 0; i < configList.length(); i++) {
                JSONObject configApp = configList.getJSONObject(i);
                if (this.projectLink.contains(configApp.getString("app"))){
                    this.config = configApp;
                    this.app = configApp.getString("app");
                    return;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject getAppFromLink(String projectLink) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(this.configPath)));
            JSONArray configList = new JSONArray(content);
            for (int i = 0; i < configList.length(); i++) {
                JSONObject configApp = configList.getJSONObject(i);
                if (projectLink.contains(configApp.getString("app"))){
                   return configApp;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private String buildAPILink(String projectLink, JSONObject currentConfig, String targetAction) {
        JSONArray appActions = currentConfig.getJSONArray("action");
        JSONObject triggerAction = new JSONObject();

        int userNameIndex = -1; int projectNameIndex = -1;
        for (int i = 0, size = appActions.length(); i < size; i++){
            JSONObject action = appActions.getJSONObject(i);
            if (action.getString("name").equals(targetAction)) {
                triggerAction = action;
                String[] projectLinkPattern = action.getString("projectLink").split("/");
                userNameIndex = Arrays.binarySearch(projectLinkPattern, "{userNameApp}");
                projectNameIndex = Arrays.binarySearch(projectLinkPattern, "{projectName}");
            }
        }

        String[] projectLinkComponents = projectLink.replace("https://","").replace("http://", "")
                .split("/");
        if ((userNameIndex != -1) && (projectNameIndex != -1)){
            String userNameApp = projectLinkComponents[userNameIndex];
            String projectName = projectLinkComponents[projectNameIndex];
            String apiInfo =  triggerAction.getString("apiInfo");
            if (apiInfo.contains("{userNameApp}"))
                apiInfo = apiInfo.replace("{userNameApp}", userNameApp);
            if (apiInfo.contains("{projectName}"))
                apiInfo = apiInfo.replace("{projectName}", userNameApp);

            return apiInfo;
        }
        return "";
    }

    private JSONArray callAPI(String apiLink, JSONObject currentConfig, Bridge bridge) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        String auth = bridge.getUserNameApp() + ":" + bridge.getPasswordApp();
        byte[] encodedAuth = Base64.getEncoder().encode(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        try {
            builder = new URIBuilder(apiLink);
            String finalUri = builder.build().toString();

            int postStatusCode = -1;
            HttpResponse response = null;
            if (currentConfig.getString("method").equals("GET")) {
                HttpGet getMethod = new HttpGet(finalUri);
                getMethod.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
                response = client.execute(getMethod);

                postStatusCode = response.getStatusLine()
                        .getStatusCode();
            }

            if (postStatusCode != 200)
                return null;
            else {
                String result = EntityUtils.toString(response.getEntity());
                return new JSONArray(result);
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String traverseMessageLevel(JSONObject message, String path) {
        JSONObject tmp = new JSONObject(message);
        String result = "";
        String[] levels = path.split("\\|");

        for (String keyword : levels) {
            if (tmp.keySet().contains(keyword)) {
                if (tmp.get(keyword) instanceof String)
                    result = tmp.get(keyword).toString();
                else
                    tmp = tmp.getJSONObject(keyword);
            }
        }

        return result;
    }

    // TODO: revision
    private List<Dictionary<String, String>> extractInfo(String projectLink, JSONArray originMessage, JSONObject currentConfig) {
        JSONObject messageInfoConfig = currentConfig.getJSONObject("extraInfo");
        List<Dictionary<String, String>> extractTriggerActions = new ArrayList<>();

        for (int i = 0; i < originMessage.length(); i++) {
            JSONObject triggeredAction = originMessage.getJSONObject(i);
            Dictionary<String, String> extractInfo = new Hashtable<>();

            extractInfo.put("id", this.traverseMessageLevel(triggeredAction, messageInfoConfig.getString("id")));
            extractInfo.put("created_at", this.traverseMessageLevel(triggeredAction, messageInfoConfig.getString("time")));
            extractInfo.put("task", this.traverseMessageLevel(triggeredAction, messageInfoConfig.getString("task")).trim().replace("'", "\""));
            extractInfo.put("actor", this.traverseMessageLevel(triggeredAction, messageInfoConfig.getString("actor")).trim());
            extractInfo.put("project_id", projectLink);
            extractInfo.put("app", currentConfig.getString("app"));

            extractTriggerActions.add(extractInfo);
        }

        return extractTriggerActions;
    }

    public List<Dictionary<String, String>> getLatestTrigger(Boolean takeAll, List<ActionEvent> actionEvents, Bridge bridge) {
        List<Dictionary<String, String>> extractTriggerActions = new ArrayList<>();

        JSONObject repoDetected = this.getAppFromLink(this.projectLink);

        if (repoDetected == null)
            return null;

        List<String> actionList = new ArrayList<>();
        for (ActionEvent actionEvent: actionEvents) {
            if (!actionList.contains(actionEvent.getAction()))
                actionList.add(actionEvent.getAction());
        }

        for (String action: actionList) {
            String apiLink = this.buildAPILink(this.projectLink, repoDetected, action);
            JSONArray originalTriggers = this.callAPI(apiLink, repoDetected, bridge);

            if (!takeAll) {
                assert originalTriggers != null;
                JSONObject tmp = originalTriggers.getJSONObject(0);
                originalTriggers = new JSONArray();
                originalTriggers.put(tmp);
            }

            assert originalTriggers != null;
            extractTriggerActions.addAll(this.extractInfo(projectLink, originalTriggers, repoDetected));
        }

        return extractTriggerActions;
    }
}
