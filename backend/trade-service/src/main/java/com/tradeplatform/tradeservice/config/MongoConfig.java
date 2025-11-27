package com.tradeplatform.tradeservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.database:trade_audit_db}")
    private String database;

    @Value("${spring.data.mongodb.username:}")
    private String username;

    @Value("${spring.data.mongodb.password:}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database:}")
    private String authenticationDatabase;

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory() {
        String connectionString;
        
        // Build connection string with optional authentication
        // Check if username and password are provided and not empty
        boolean hasAuth = username != null 
            && !username.trim().isEmpty() 
            && !username.equals("${MONGO_USERNAME}")
            && password != null 
            && !password.trim().isEmpty()
            && !password.equals("${MONGO_PASSWORD}");
        
        if (hasAuth) {
            // With authentication
            String authDb = (authenticationDatabase != null 
                && !authenticationDatabase.trim().isEmpty() 
                && !authenticationDatabase.equals("${MONGO_AUTH_DB}")) 
                ? authenticationDatabase : "admin";
            connectionString = String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s",
                username, password, host, port, database, authDb);
        } else {
            // Without authentication (for local development)
            connectionString = String.format("mongodb://%s:%d/%s", host, port, database);
        }
        
        return new SimpleMongoClientDatabaseFactory(connectionString);
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }
}

