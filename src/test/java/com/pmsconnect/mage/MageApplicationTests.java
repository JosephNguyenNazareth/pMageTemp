package com.pmsconnect.mage;

import com.pmsconnect.mage.connector.Connector;
import com.pmsconnect.mage.connector.ConnectorRepository;
import com.pmsconnect.mage.connector.ConnectorService;
import com.pmsconnect.mage.user.Bridge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
class MageApplicationTests {
	@Autowired
	private ConnectorRepository connectorRepository;

	@Autowired
	private ConnectorService connectorService;

	@Test
	void createNewConnection() {
		// Connection declarations ----------------------------------
		Bridge bridge1 = new Bridge("github.com", "sunny", "abcd",
				"", "/Users/nguyenjoseph/Documents/Pop_Documents/workspace/chasca/hello",
				"sunny", "abcd", "core-bape", "http://localhost:8090/api/process-instance", "9f9f28d8-9e91-4d55-b75b-a94831d8fd37");
		String connectorIdP1 = connectorService.addNewConnector(bridge1);
		Connector connectorP1 = connectorService.getConnector(connectorIdP1);
		connectorRepository.save(connectorP1);
	}

}
