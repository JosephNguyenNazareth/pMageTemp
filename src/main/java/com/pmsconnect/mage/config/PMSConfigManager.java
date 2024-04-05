package com.pmsconnect.mage.config;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PMSConfigManager {
    private List<PmsConfig> listPMSConfig;
    private List<String> listPMSName;
    private String configPath;

    public PMSConfigManager(String configPath) {
        this.configPath = configPath;
        this.listPMSConfig = new ArrayList<>();
        this.listPMSName = new ArrayList<>();
        this.loadPMSConfig();
    }

    public void loadPMSConfig() {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(this.configPath));
            JSONArray configList = (JSONArray)obj;
            for (Object o : configList) {
                JSONObject configPms = (JSONObject) o;
                String pmsName = configPms.get("pms").toString();
                this.listPMSName.add(pmsName);
                this.listPMSConfig.add(new PmsConfig(this.configPath, pmsName, configPms));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public List<PmsConfig> getListPMSConfig() {
        return listPMSConfig;
    }

    public List<String> getListPMSName() {
        return listPMSName;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void addPMSConfig(JSONObject configPMS) {
        String pmsName = configPMS.get("pms").toString();
        this.listPMSName.add(pmsName);
        this.listPMSConfig.add(new PmsConfig(this.configPath, pmsName, configPMS));
    }

    public String addPMSConfig(String configPMS) {
//        String pmsName = configPMS.get("pms").toString();
//        this.listPMSName.add(pmsName);
//        this.listPMSConfig.add(new PmsConfig(this.configPath, pmsName, configPMS));
        System.out.println(configPMS);
        return this.parsePMSConfig(configPMS);
    }

    private String parsePMSConfig(String configPMS) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject pmsObject = (JSONObject) parser.parse(configPMS);

            JSONObject finalPMSConfig = new JSONObject();
            Iterator<String> keys = pmsObject.keySet().iterator();
            JSONObject apiInfo = new JSONObject();
            String pmsName = "";

            while(keys.hasNext()) {
                String key = keys.next();
                if (key.equals("pmsName")) {
                    pmsName = pmsObject.get(key).toString();
                    if (this.listPMSName.contains(pmsName))
                        return "ERROR: Duplicated PMS Configuration.";
                    finalPMSConfig.put("pms", pmsObject.get(key));
                    continue;
                } else if (key.equals("pmsUrl")) {
                    finalPMSConfig.put("url", pmsObject.get(key));
                    continue;
                }

                String[] funcInfo = key.split("_");
                JSONObject attr = new JSONObject();
                attr.put(funcInfo[1], pmsObject.get(key));
                apiInfo.put(funcInfo[0], attr);
            }
            finalPMSConfig.put("api_info", apiInfo);

            this.listPMSName.add(pmsName);
            this.listPMSConfig.add(new PmsConfig(this.configPath, pmsName, finalPMSConfig));

            return "Successfully updated";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
