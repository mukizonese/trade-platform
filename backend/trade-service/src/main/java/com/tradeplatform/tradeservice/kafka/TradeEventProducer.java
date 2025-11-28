package com.tradeplatform.tradeservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(KafkaTemplate.class)
public class TradeEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TRADE_REPORTS_TOPIC = "trade-reports";
    
    public void sendTradeEvent(String eventType, String tradeId, Integer version, 
                               Map<String, Object> payload, String reason) {
        try {
            Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", eventType,
                "tradeId", tradeId,
                "version", version,
                "timestamp", java.time.Instant.now().toString(),
                "payload", payload,
                "reason", reason != null ? reason : ""
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Send to trade-reports topic
            CompletableFuture<SendResult<String, Object>> reportsFuture = 
                kafkaTemplate.send(TRADE_REPORTS_TOPIC, tradeId, event);
            
            reportsFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Sent trade event to {}: {}", TRADE_REPORTS_TOPIC, eventJson);
                } else {
                    log.error("Failed to send trade event to {}", TRADE_REPORTS_TOPIC, ex);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing trade event", e);
        }
    }
}

