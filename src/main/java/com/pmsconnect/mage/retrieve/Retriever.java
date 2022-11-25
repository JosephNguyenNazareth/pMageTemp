package com.pmsconnect.mage.retrieve;

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

public class Retriever {
    private List<String> repoList;
    private JSONArray config;
    private String configPath;

    public Retriever(String configPath) {
        this.repoList = new ArrayList<>();
        this.configPath = configPath;
        this.readConfig();
    }

    public void addRepo(String repoLink) {
        this.repoList.add(repoLink);
    }

    private void addRepoList(List<String> repoList) {
        this.repoList.addAll(repoList);
    }

    private void readConfig() {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(this.configPath));
            this.config = (JSONArray)obj;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject getRepoOrigin(String repoLink) {
        for (int i = 0; i < this.config.size(); i++) {
            JSONObject jObject = (JSONObject) this.config.get(i);
            String origin = jObject.get("origin").toString();
            if (repoLink.contains(origin))
                return jObject;
        }
        return null;
    }

    private String buildAPILinkCommit(String repoLink, JSONObject currentConfig) {
        JSONObject configAPIInfo = (JSONObject) currentConfig.get("api_info");
        String origin = currentConfig.get("origin").toString();

        if (origin.equals("github.com"))
            return repoLink.replace(origin, configAPIInfo.get("api_prefix").toString()) + "/" + configAPIInfo.get("api_postfix_commit").toString();
        else if (origin.equals("gitlab.com")) {
            String urlPath = repoLink.substring(repoLink.indexOf(origin) + origin.length() + 1).replace("/", "%2F");
            return "http://" + configAPIInfo.get("api_prefix") + "/" + urlPath + "/" + configAPIInfo.get("api_postfix_commit");
        }
        return "";
    }

    private JSONArray callAPI(String apiLink, JSONObject currentConfig) {
        JSONObject configInfo = (JSONObject) currentConfig.get("user_info");
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
                    tmp = (JSONObject) tmp.get(keyword);
            }
        }

        return result;
    }

    private List<Dictionary<String, String>> extractInfoCommit(String repoLink, JSONArray originMessage, JSONObject currentConfig) {
        JSONObject messageInfoConfig = (JSONObject) currentConfig.get("message_info");
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

        for (String repoLink : this.repoList) {
            JSONObject repoDetected = this.getRepoOrigin(repoLink);

            String apiLink = this.buildAPILinkCommit(repoLink, repoDetected);
            JSONArray originalCommits = this.callAPI(apiLink, repoDetected);

            if (!takeAll) {
                JSONObject tmp = (JSONObject) originalCommits.get(0);
                originalCommits = new JSONArray();
                originalCommits.add(tmp);
            }

            extractCommits.addAll(this.extractInfoCommit(repoLink, originalCommits, repoDetected));
        }

        return extractCommits;
    }
}
