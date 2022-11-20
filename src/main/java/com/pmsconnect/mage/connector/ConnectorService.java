package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.repo.UserRepo;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class ConnectorService {
    private final ConnectorRepository connectorRepository;

    @Autowired
    public ConnectorService(ConnectorRepository mageRepository) {
        this.connectorRepository = mageRepository;
    }

    public List<Connector> getConnectors() {
        return connectorRepository.findAll();
    }

    public String addNewConnector(String url, String pmsProjectId, UserRepo user) {
        if (!verifyPmsExist(url, pmsProjectId))
            throw new IllegalStateException("Cannot verify pms url " + url);
        Connector connector = new Connector(url, pmsProjectId, user);

        connectorRepository.save(connector);

        return connector.getId();
    }

    @Transactional
    public void updateConnector(String connectorId, String pmsProjectId, UserRepo user) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        if (pmsProjectId != null && !pmsProjectId.equals(connector.getPmsProjectId())) {
            if (!verifyPmsExist(connector.getUrl(), pmsProjectId))
                throw new IllegalStateException("Cannot verify pms projectId " + pmsProjectId);
            connector.setPmsProjectId(pmsProjectId);
        }

        if (user != null && !user.equals(connector.getUserRepo()))
            connector.setUserRepo(user);

        connectorRepository.save(connector);
    }

    private boolean verifyPmsExist(String url, String pmsProjectId) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            if (pmsProjectId != null)
                builder = new URIBuilder(url + "/" + pmsProjectId);
            else
                builder = new URIBuilder(url);
            String finalUri = builder.build().toString();
            HttpGet getMethod = new HttpGet(finalUri);
            HttpResponse getResponse = client.execute(getMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode == 200)
                return true;
            else
                return false;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public String createProcessInstance(String connectorId, String processName) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        if (!createPMSProcessInstance(connector, processName, connector.getUserRepo().getRealName()))
            throw new IllegalStateException("Cannot create new process instance");
        connectorRepository.save(connector);

        return connector.getPmsProjectId();
    }

    private boolean createPMSProcessInstance(Connector connector, String processName, String creatorName) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(connector.getUrl());
            builder.addParameter("processName", processName);
            builder.addParameter("creatorName", creatorName);
            String finalUri = builder.build().toString();
            HttpPost postMethod = new HttpPost(finalUri);
            HttpResponse getResponse = client.execute(postMethod);

            int postStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (postStatusCode != 200)
                return false;
            else {
                String responseBody = EntityUtils.toString(getResponse.getEntity());
                connector.setPmsProjectId(responseBody);
                return true;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopMonitoringProcessInstance(String connectorId) {
        Connector connector = connectorRepository.findById(connectorId).orElseThrow(() -> new IllegalStateException("Connector with id " + connectorId + "does not exist."));

        connector.setMonitoring(false);
        connectorRepository.save(connector);
        this.closeProcess(connector);
        System.out.println("Stop monitoring connector with id " + connectorId);
    }

    private void closeProcess(Connector connector) {
        HttpClient client = HttpClients.createDefault();
        URIBuilder builder = null;
        try {
            builder = new URIBuilder(connector.getUrl() + "/" + connector.getPmsProjectId() + "/change-state");
            builder.addParameter("processInstanceState", Boolean.toString(true));

            String finalUri = builder.build().toString();
            HttpPut putMethod = new HttpPut(finalUri);
            HttpResponse getResponse = client.execute(putMethod);

            int getStatusCode = getResponse.getStatusLine()
                    .getStatusCode();
            if (getStatusCode != 200)
                throw new IllegalStateException("Cannot close process instance id " + connector.getPmsProjectId());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
