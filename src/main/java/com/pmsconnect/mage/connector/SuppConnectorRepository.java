package com.pmsconnect.mage.connector;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuppConnectorRepository extends MongoRepository<Connector, String> {
}
