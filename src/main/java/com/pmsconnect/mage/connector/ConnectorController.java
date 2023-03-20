package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.user.Bridge;
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
}
