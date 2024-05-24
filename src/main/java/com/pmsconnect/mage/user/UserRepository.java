package com.pmsconnect.mage.user;

import com.pmsconnect.mage.connector.Connector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
}
