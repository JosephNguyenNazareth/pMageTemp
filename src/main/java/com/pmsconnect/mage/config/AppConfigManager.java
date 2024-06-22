package com.pmsconnect.mage.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AppConfigManager {
    private List<AppConfig> listAppConfig;
    private List<String> listAppName;
    private String configPath;

    public AppConfigManager(String configPath) {
        this.configPath = configPath;
        this.listAppConfig = new ArrayList<>();
        this.listAppName = new ArrayList<>();
        this.loadAppConfig();
    }

    public void loadAppConfig() {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(this.configPath));
            JSONArray configList = (JSONArray)obj;
            for (Object o : configList) {
                JSONObject configApp = (JSONObject) o;
                String appName = configApp.get("origin").toString();
                this.listAppName.add(appName);
                this.listAppConfig.add(new AppConfig(this.configPath, appName, configApp));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public List<AppConfig> getListAppConfig() {
        return listAppConfig;
    }

    public List<String> getListAppName() {
        return listAppName;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void addAppConfig(JSONObject configApp) {
        String appName = configApp.get("origin").toString();
        this.listAppName.add(appName);
        this.listAppConfig.add(new AppConfig(this.configPath, appName, configApp));
    }

    public String addAppConfig(String configApp) {
//        String appName = configApp.get("app").toString();
//        this.listAppName.add(appName);
//        this.listAppConfig.add(new AppConfig(this.configPath, appName, configApp));
        System.out.println(configApp);
        return this.parseAppConfig(configApp);
    }

    private String parseAppConfig(String configApp) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject appObject = (JSONObject) parser.parse(configApp);

            JSONObject finalAppConfig = new JSONObject();
            Iterator<String> keys = appObject.keySet().iterator();
            JSONObject apiInfo = new JSONObject();
            String appName = "";

            while(keys.hasNext()) {
                String key = keys.next();
                if (key.equals("appName")) {
                    appName = appObject.get(key).toString();
                    if (this.listAppName.contains(appName))
                        return "ERROR: Duplicated App Configuration.";
                    finalAppConfig.put("app", appObject.get(key));
                    continue;
                } else if (key.equals("appUrl")) {
                    finalAppConfig.put("url", appObject.get(key));
                    continue;
                }

                String[] funcInfo = key.split("_");
                JSONObject attr = new JSONObject();
                attr.put(funcInfo[1], appObject.get(key));
                apiInfo.put(funcInfo[0], attr);
            }
            finalAppConfig.put("api_info", apiInfo);

            this.listAppName.add(appName);
            this.listAppConfig.add(new AppConfig(this.configPath, appName, finalAppConfig));

            return "Successfully updated";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
