package com.pmsconnect.mage.config;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PmsConfig {
    private String pms;
    private JSONObject config;
    private String configPath;

    public PmsConfig() {
        System.out.println("hello");
    }

    public PmsConfig(String configPath) {
        this.configPath = configPath;
    }

    public PmsConfig(String configPath, String pms) {
        this.configPath = configPath;
        this.pms = pms;
        this.readConfig();
    }

    public PmsConfig(String configPath, String pms, JSONObject config) {
        this.configPath = configPath;
        this.pms = pms;
        this.config = config;
    }

    public String getPms() {
        return pms;
    }

    public void setPms(String pms) {
        this.pms = pms;
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

    public void readConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(this.configPath)));
            JSONArray configList = new JSONArray(content);
            for (int i = 0; i < configList.length(); i++) {
                JSONObject configPms = configList.getJSONObject(i);
                if (configPms.getString("pms").equals(this.pms)){
                    this.config = configPms;
                    return;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String getUrl() {
        return this.config.getString("url");
    }

    public String buildAPI(String function, Map<String, String> url, Map<String, String> param) throws URISyntaxException {
        JSONObject infoAPI =  this.config.getJSONObject("api_info");
        if (!infoAPI.keySet().contains(function))
            return "";

        JSONObject functionInfo = infoAPI.getJSONObject(function);

        String urlToBeReplaced = functionInfo.getString("url");
        int index = urlToBeReplaced.indexOf("{");
        while (index >= 0) {
            String keyword = urlToBeReplaced.substring(index + 1, urlToBeReplaced.indexOf("}"));
            String keywordReplaced = urlToBeReplaced.substring(index, urlToBeReplaced.indexOf("}") + 1);
            if (url.containsKey(keyword))
                urlToBeReplaced = urlToBeReplaced.replace(keywordReplaced, url.get(keyword));
            else {
                System.out.println("Cannot find matching keyword to build API call.");
                return "";
            }

            index = urlToBeReplaced.indexOf("{", index + 1);
        }

        URIBuilder builder = new URIBuilder(urlToBeReplaced);
        for (Map.Entry<String, String> entry : param.entrySet()) {
            builder.addParameter(entry.getKey(), entry.getValue());
        }

        return builder.build().toString();
    }

    private void provideFixedHeaders(JSONObject infoHeader, Map<String, String> headerSet) {
        JSONObject infoFixed = infoHeader.getJSONObject("fixed");
        Iterator<String> keys = infoFixed.keySet().iterator();

        while(keys.hasNext()) {
            String key = keys.next();
            String value  = infoFixed.getString(key);
            headerSet.put(key, value);
        }
    }

    private void provideDynamicHeaders(JSONObject infoHeader, Map<String, String> headerSet) {
        JSONObject infoDynamic = infoHeader.getJSONObject("dynamic");

        Iterator<String> keys = infoDynamic.keySet().iterator();

        while(keys.hasNext()) {
            String key = keys.next();
            JSONObject sourceInfo = infoDynamic.getJSONObject(key);
            Iterator<String> keysSource = sourceInfo.keySet().iterator();
            while (keysSource.hasNext()) {
                String key2 = keysSource.next();
                String value = sourceInfo.getString(key2);
                headerSet.put(key, key2 + ":" + value);
            }
        }
    }

    public Map<String, String> provideExtraInfo(String infoPlace, String function) {
        JSONObject infoAPI = this.config.getJSONObject("api_info");
        if (!infoAPI.keySet().contains(function))
            return null;

        JSONObject infoFunction = infoAPI.getJSONObject(function);
        if (!infoFunction.keySet().contains(infoPlace))
            return null;

        JSONObject infoHeader = infoFunction.getJSONObject(infoPlace);
        Map<String, String> headerSet = new HashMap<>();

        if (infoHeader.keySet().contains("fixed")) {
            provideFixedHeaders(infoHeader, headerSet);
        }
        if (infoHeader.keySet().contains("dynamic")) {
            provideDynamicHeaders(infoHeader, headerSet);
        }
        return headerSet;
    }
}
