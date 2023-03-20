package com.pmsconnect.mage.config;

import org.apache.http.client.utils.URIBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PmsConfig {
    private String pms;
    private JSONObject config;
    private String configPath;

    public PmsConfig(String configPath, String pms) {
        this.configPath = configPath;
        this.pms = pms;
        this.readConfig();
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

    private void readConfig() {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(this.configPath));
            JSONArray configList = (JSONArray)obj;
            for (Object o : configList) {
                JSONObject configPms = (JSONObject) o;
                if (configPms.get("pms").toString().equals(this.pms)){
                    this.config = configPms;
                    return;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String getUrlPMS() {
        return this.config.get("url").toString();
    }

    public String buildAPI(String function, Map<String, String> url, Map<String, String> param) throws URISyntaxException {
        JSONObject infoAPI = new JSONObject((LinkedHashMap)this.config.get("api_info"));
        if (!infoAPI.containsKey(function))
            return "";

        JSONObject functionInfo = new JSONObject((LinkedHashMap)infoAPI.get(function));

        String urlToBeReplaced = functionInfo.get("url").toString();
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
}
