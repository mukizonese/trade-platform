package com.tradeplatform.tradeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {"com.tradeplatform.tradeservice.model.entity"})
@EnableJpaRepositories(basePackages = {"com.tradeplatform.tradeservice.repository"})
@EnableMongoRepositories(basePackages = {"com.tradeplatform.tradeservice.repository"})
@EnableMongoAuditing
public class TradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}

