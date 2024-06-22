package com.pmsconnect.mage.config;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AppConfig {
    private String projectLink;
    private JSONObject config;
    private String configPath;
    private String app;

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
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(this.configPath));
            JSONArray configList = (JSONArray)obj;
            for (Object o : configList) {
                JSONObject configApp = (JSONObject) o;
                if (this.projectLink.contains(configApp.get("origin").toString())){
                    this.config = configApp;
                    this.app = configApp.get("origin").toString();
                    return;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject getRepoOrigin(String repoLink) {
        for (int i = 0; i < this.config.size(); i++) {
            JSONObject jObject =  new JSONObject((LinkedHashMap)this.config.get(i));
            String origin = jObject.get("origin").toString();
            if (repoLink.contains(origin))
                return jObject;
        }
        return null;
    }

    private String buildAPILinkCommit(String repoLink, JSONObject currentConfig) {
        JSONObject configAPIInfo = new JSONObject((LinkedHashMap) currentConfig.get("api_info"));
        String origin = currentConfig.get("origin").toString();

        if (origin.equals("github.com"))
            return repoLink.replace(origin, configAPIInfo.get("api_prefix").toString()) + "/" + configAPIInfo.get("api_postfix_commit").toString();
        else if (origin.equals("gitlab.com")) {
            String urlPath = repoLink.substring(repoLink.indexOf(origin) + origin.length() + 1).replace("/", "%2F");
            return "https://" + configAPIInfo.get("api_prefix") + "/" + urlPath + "/" + configAPIInfo.get("api_postfix_commit");
        }
        return "";
    }

    private String buildAPILinkRevertCommit(String commitId, String repoLink, JSONObject currentConfig) {
        JSONObject configAPIInfo = new JSONObject((LinkedHashMap) currentConfig.get("api_info"));
        String origin = currentConfig.get("origin").toString();

        if (origin.equals("github.com"))
            return "";
        else if (origin.equals("gitlab.com")) {
            String urlPath = repoLink.substring(repoLink.indexOf(origin) + origin.length() + 1).replace("/", "%2F");
            return "https://" + configAPIInfo.get("api_prefix") + "/" + urlPath + "/" + configAPIInfo.get("api_postfix_commit") + "/" + commitId + "/" + configAPIInfo.get("api_postfix_revert");
        }
        return "";
    }

    private JSONArray callAPI(String apiLink, JSONObject currentConfig) {
        JSONObject configInfo = new JSONObject((LinkedHashMap) currentConfig.get("user_info"));
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        String auth = configInfo.get("user").toString() + ":" + configInfo.get("token").toString();
        byte[] encodedAuth = Base64.getEncoder().encode(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        try {
            builder = new URIBuilder(apiLink);
            String finalUri = builder.build().toString();
            HttpGet getMethod = new HttpGet(finalUri);
            getMethod.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            HttpResponse getResponse = client.execute(getMethod);

            int postStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (postStatusCode != 200)
                return null;
            else {
                String result = EntityUtils.toString(getResponse.getEntity());
                JSONParser jParser = new JSONParser();
                Object obj = jParser.parse(result);
                return (JSONArray) obj;
            }
        } catch (URISyntaxException | IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private String traverseMessageLevel(JSONObject message, String path) {
        JSONObject tmp = new JSONObject(message);
        String result = "";
        String[] levels = path.split("\\|");

        for (String keyword : levels) {
            if (tmp.containsKey(keyword)) {
                if (tmp.get(keyword) instanceof String)
                    result = tmp.get(keyword).toString();
                else
                    tmp = new JSONObject((LinkedHashMap) tmp.get(keyword));
            }
        }

        return result;
    }

    private List<Dictionary<String, String>> extractInfoCommit(String repoLink, JSONArray originMessage, JSONObject currentConfig) {
        JSONObject messageInfoConfig = new JSONObject((LinkedHashMap) currentConfig.get("message_info"));
        List<Dictionary<String, String>> extractCommits = new ArrayList<>();

        for (Object object : originMessage) {
            JSONObject commit = (JSONObject) object;
            Dictionary<String, String> extractInfo = new Hashtable<>();

            extractInfo.put("id", this.traverseMessageLevel(commit, messageInfoConfig.get("id").toString()));
            extractInfo.put("created_at", this.traverseMessageLevel(commit, messageInfoConfig.get("time").toString()));
            extractInfo.put("title", this.traverseMessageLevel(commit, messageInfoConfig.get("title").toString()).trim().replace("'", "\""));
            extractInfo.put("committer_name", this.traverseMessageLevel(commit, messageInfoConfig.get("committer").toString()).trim());
            extractInfo.put("project_id", repoLink);
            extractInfo.put("origin", currentConfig.get("origin").toString());

            extractCommits.add(extractInfo);
        }

        return extractCommits;
    }

    public List<Dictionary<String, String>> getLatestCommitLog(Boolean takeAll) {
        List<Dictionary<String, String>> extractCommits = new ArrayList<>();

        JSONObject repoDetected = this.getRepoOrigin(projectLink);

        String apiLink = this.buildAPILinkCommit(projectLink, repoDetected);
        JSONArray originalCommits = this.callAPI(apiLink, repoDetected);

        if (!takeAll) {
            JSONObject tmp = (JSONObject) originalCommits.get(0);
            originalCommits = new JSONArray();
            originalCommits.add(tmp);
        }

        extractCommits.addAll(this.extractInfoCommit(projectLink, originalCommits, repoDetected));

        return extractCommits;
    }
}
