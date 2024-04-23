package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.user.Bridge;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "connector")
public class SupplementaryConnector extends Connector {
    private String suppAppName;
    private String suppProjectLink;
    private String suppProjectDir;

    public SupplementaryConnector() {
        super();
    }

    public SupplementaryConnector(Bridge bridge, String suppAppName, String suppProjectDir, String suppProjectLink) {
        super(bridge);
        this.suppAppName = suppAppName;
        this.suppProjectDir = suppProjectDir;
        this.suppProjectLink = suppProjectLink;
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
}
