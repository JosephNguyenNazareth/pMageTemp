package com.pmsconnect.mage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
@EnableMongoRepositories
@EnableAsync
public class MageApplication {

	public static void main(String[] args) {
		SpringApplication.run(MageApplication.class, args);
	}

}
