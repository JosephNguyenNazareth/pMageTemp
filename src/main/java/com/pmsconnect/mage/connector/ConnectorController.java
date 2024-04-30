package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.user.Bridge;
import com.pmsconnect.mage.user.PMSConnection;
import com.pmsconnect.mage.utils.Alignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Dictionary;
import java.util.List;

@RestController
@RequestMapping(path = "api/pmage")
public class ConnectorController {
    private final ConnectorService connectorService;
    private final ConnectorAsyncService connectorAsyncService;

    @Autowired
    public ConnectorController(ConnectorService connectorService, ConnectorAsyncService connectorAsyncService) {
        this.connectorService = connectorService;
        this.connectorAsyncService = connectorAsyncService;
    }

    @GetMapping
    public List<Connector> getConnectors() {
        return connectorService.getConnectors();
    }

    @GetMapping(path = "{connectorId}")
    public Connector getConnector(@PathVariable("connectorId") String connectorId) {
        return connectorService.getConnector(connectorId);
    }

    @GetMapping(path = "/user/{userName}")
    public List<Connector> getConnectorsByUserName(@PathVariable("userName") String userName) {
        return connectorService.getConnectorsByUserName(userName);
    }


    @PostMapping(path = "/add-supp/{connectorId}")
    public String addSupplementaryConnector(
            @PathVariable("connectorId") String connectorId,
            @RequestBody Bridge bridge) {
        return connectorService.addSupplementaryConnectors(bridge, connectorId);
    }

    @PostMapping(path = "/add")
    public String addNewConnector(
            @RequestBody Bridge bridge) {
        return connectorService.addNewConnector(bridge);
    }

    @PutMapping(path = "/update/{connectorId}")
    public void updateConnector(
            @PathVariable("connectorId") String connectorId,
            @RequestBody(required = false) Bridge bridge) {
        connectorService.updateConnector(connectorId, bridge);
    }

    @PutMapping(path = "/update-supp/{connectorId}")
    public void updateSuppConnector(
            @PathVariable("connectorId") String connectorId,
            @RequestBody(required = false) Bridge bridge,
            @RequestParam String baseConnectorId) {
        connectorService.updateSuppConnector(connectorId, bridge, baseConnectorId);
    }

    @DeleteMapping(path = "/delete/{connectorId}")
    public void deleteConnector(
            @PathVariable("connectorId") String connectorId) {
        connectorService.deleteConnector(connectorId);
    }

    @PutMapping(path = "{connectorId}/create-process")
    public String createProcessInstance(
            @PathVariable("connectorId") String connectorId,
            @RequestParam String processName) {
        return connectorService.createProcessInstance(connectorId, processName);
    }

    @GetMapping(path = "{connectorId}/get-process")
    public String getProcessInstance(
            @PathVariable("connectorId") String connectorId) {
        return connectorService.getProcessInstance(connectorId);
    }

    @GetMapping(path = "{connectorId}/monitor")
    public void monitorProcessInstance(
            @PathVariable("connectorId") String connectorId) {
        connectorAsyncService.monitorProcessInstance(connectorId);
    }

    @GetMapping(path = "{connectorId}/end-monitor")
    public void stopMonitoringProcessInstance(
            @PathVariable("connectorId") String connectorId) {
        connectorService.stopMonitoringProcessInstance(connectorId);
    }

    @GetMapping(path = "{connectorId}/all-commit")
    public List<Dictionary<String, String>> getLatestCommit(
            @PathVariable("connectorId") String connectorId) {
        return connectorAsyncService.getAllCommit(connectorId);
    }

    @PutMapping(path = "{connectorId}/add-table")
    public void addActionTable(@PathVariable("connectorId") String connectorId,
                               @RequestParam String actionDescription) {
        connectorService.addActionEventTable(connectorId, actionDescription);
    }

    // auto generate the keyword using information form connected process instance
    @GetMapping(path = "{connectorId}/generate-table")
    public String generateActionTable(@PathVariable("connectorId") String connectorId) {
        return connectorService.generateActionEventTable(connectorId);
    }

    @GetMapping(path = "{connectorId}/history")
    public void loadHistory(@PathVariable("connectorId") String connectorId) {
        connectorService.loadHistoryCommit(connectorId);
    }

    @GetMapping(path = "pms-config")
    public List<String> getPMSConfig() {
        return connectorService.getPMSConfig();
    }


    @PostMapping(path = "pms-config")
    public String updatePMSConfig(@RequestBody String pmsConfig) {
        return connectorService.addPMSConfig(pmsConfig);
    }

    @GetMapping(path = "{connectorId}/collect-hist")
    public List<Alignment> getConnectorHist(@PathVariable String connectorId) {
        return connectorService.getConnectorHist(connectorId);
    }

    @GetMapping(path = "caseid")
    public List<String> login(@RequestParam String pmsName,
                      @RequestParam String pmsURL,
                      @RequestParam String usernamePMS,
                      @RequestParam String passwordPMS,
                      @RequestParam String processDef){
        return connectorService.getProcessInstanceIdList(pmsName, pmsURL, usernamePMS, passwordPMS, processDef);
    }
}
