package com.tradeplatform.tradeservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", matchIfMissing = false)
@Slf4j
public class KafkaConnectionConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    @Primary
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        log.info("Creating Kafka ProducerFactory with bootstrap-servers: '{}'", bootstrapServers);
        
        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            log.error("Kafka bootstrap-servers is empty or null! Check that KAFKA_BOOTSTRAP_SERVERS environment variable is set.");
            throw new IllegalStateException("Kafka bootstrap-servers cannot be empty. Set KAFKA_BOOTSTRAP_SERVERS environment variable.");
        }
        
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                org.springframework.kafka.support.serializer.JsonSerializer.class);
        
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000); // 10 second timeout 0 Connection timeout - don't block too long on startup
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        log.info("Kafka ProducerFactory configured successfully with bootstrap-servers: {}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        log.info("Creating KafkaTemplate with bootstrap-servers: {}", bootstrapServers);
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        log.info("KafkaTemplate created successfully");
        return template;
    }
}

