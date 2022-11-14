package com.pmsconnect.mage.connector;

import com.pmsconnect.mage.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "api/mage")
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

    @PostMapping
    public String addNewConnector(
            @RequestParam String url,
            @RequestParam(required = false) String pmsProjectId,
            @RequestBody(required = false) UserRepo user) {
        return connectorService.addNewConnector(url, pmsProjectId, user);
    }

    @PutMapping(path = "{connectorId}")
    public void updateConnector(
            @PathVariable("connectorId") String connectorId,
            @RequestParam(required = false) String pmsProjectId,
            @RequestBody(required = false) UserRepo user) {
        connectorService.updateConnector(connectorId, pmsProjectId, user);
    }

    @PutMapping(path = "{connectorId}/create-process")
    public String createProcessInstance(
            @PathVariable("connectorId") String connectorId,
            @RequestParam(required = false) String processName) {
        return connectorService.createProcessInstance(connectorId, processName);
    }

    @GetMapping(path = "{connectorId}/monitor")
    public void monitorProcessInstance(
            @PathVariable("connectorId") String connectorId) {
        connectorAsyncService.monitorProcessInstance(connectorId);
    }
}
