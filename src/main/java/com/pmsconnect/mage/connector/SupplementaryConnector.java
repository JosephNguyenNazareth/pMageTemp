package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.user.Bridge;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Document(collection = "connector")
public class SupplementaryConnector extends Connector {
    private String suppConnectorId;
    private String suppAppName;
    private String suppProjectLink;
    private String suppProjectDir;
    private List<String> suppProjectArtifacts;

    public SupplementaryConnector() {
        super();
    }

    public SupplementaryConnector(Bridge bridge, String suppConnectorId,  String suppAppName, String suppProjectDir, String suppProjectLink) {
        super(bridge);
        this.suppConnectorId = suppConnectorId;
        this.suppAppName = suppAppName;
        this.suppProjectDir = suppProjectDir;
        this.suppProjectLink = suppProjectLink;
        this.suppProjectArtifacts = new ArrayList<>();
    }

    public SupplementaryConnector(Bridge bridge, String artifactList) {
        super(bridge);
        String[] artifactNameList = artifactList.split(",");
        this.suppProjectArtifacts = Collections.singletonList(artifactList);
    }

    public String getSuppConnectorId() {
        return suppConnectorId;
    }

    public void setSuppConnectorId(String suppConnectorId) {
        this.suppConnectorId = suppConnectorId;
    }

    public String getSuppAppName() {
        return suppAppName;
    }

    public void setSuppAppName(String suppAppName) {
        this.suppAppName = suppAppName;
    }

    public String getSuppProjectLink() {
        return suppProjectLink;
    }

    public void setSuppProjectLink(String suppProjectLink) {
        this.suppProjectLink = suppProjectLink;
    }

    public String getSuppProjectDir() {
        return suppProjectDir;
    }

    public void setSuppProjectDir(String suppProjectDir) {
        this.suppProjectDir = suppProjectDir;
    }

    public List<String> getSuppProjectArtifacts() {
        return suppProjectArtifacts;
    }

    public void setSuppProjectArtifacts(List<String> suppProjectArtifacts) {
        this.suppProjectArtifacts = suppProjectArtifacts;
    }
}
