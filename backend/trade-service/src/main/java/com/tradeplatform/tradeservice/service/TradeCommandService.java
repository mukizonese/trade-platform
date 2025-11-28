package com.tradeplatform.tradeservice.service;

import com.tradeplatform.tradeservice.dto.TradeDto;
import com.tradeplatform.tradeservice.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeservice.kafka.TradeEventProducer;
import com.tradeplatform.tradeservice.model.document.TradeAuditLog;
import com.tradeplatform.tradeservice.model.entity.Trade;
import com.tradeplatform.tradeservice.repository.TradeAuditLogRepository;
import com.tradeplatform.tradeservice.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TradeCommandService {
    
    private final TradeRepository tradeRepository;
    private final TradeAuditLogRepository auditLogRepository;
    private final CacheService cacheService;
    private final TradeEventProducer eventProducer;
    
    public TradeCommandService(
            TradeRepository tradeRepository,
            TradeAuditLogRepository auditLogRepository,
            CacheService cacheService,
            @Autowired(required = false) TradeEventProducer eventProducer) {
        this.tradeRepository = tradeRepository;
        this.auditLogRepository = auditLogRepository;
        this.cacheService = cacheService;
        this.eventProducer = eventProducer;
    }
    
    private static final String EVENT_TYPE_ACCEPTED = "TRADE_ACCEPTED";
    private static final String EVENT_TYPE_REJECTED = "TRADE_REJECTED";
    private static final String REASON_LOWER_VERSION = "LOWER_VERSION";
    private static final String REASON_PAST_MATURITY = "PAST_MATURITY";
    private static final String VALIDATION_STATUS_ACCEPTED = "ACCEPTED";
    private static final String VALIDATION_STATUS_REJECTED = "REJECTED";
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Transactional
    public TradeSubmissionResponse submitTrade(TradeDto tradeDto, String source) {
        log.info("Submitting trade: tradeId={}, version={}, source={}", 
                tradeDto.getTradeId(), tradeDto.getVersion(), source);
        
        // Check maturity rule first
        if (isPastMaturity(tradeDto.getMaturityDate())) {
            return handleRejection(tradeDto, source, REASON_PAST_MATURITY);
        }
        
        // Get current max version
        Optional<Integer> maxVersionOpt = tradeRepository.findMaxVersionByTradeId(tradeDto.getTradeId());
        Integer currentMaxVersion = maxVersionOpt.orElse(null);
        
        // Apply versioning rule
        if (currentMaxVersion == null) {
            // No existing trade - accept and store
            return handleAcceptance(tradeDto, source);
        } else if (tradeDto.getVersion() < currentMaxVersion) {
            // Lower version - reject
            return handleRejection(tradeDto, source, REASON_LOWER_VERSION);
        } else if (tradeDto.getVersion().equals(currentMaxVersion)) {
            // Same version - replace (override)
            return handleAcceptance(tradeDto, source);
        } else {
            // Higher version - accept as new version
            return handleAcceptance(tradeDto, source);
        }
    }
    
    private boolean isPastMaturity(LocalDate maturityDate) {
        return maturityDate.isBefore(LocalDate.now());
    }
    
    private TradeSubmissionResponse handleAcceptance(TradeDto tradeDto, String source) {
        log.info("Trade accepted: tradeId={}, version={}", 
                tradeDto.getTradeId(), tradeDto.getVersion());
        
        // Find existing trade if same version (for update)
        Optional<Trade> existingTradeOpt = tradeRepository.findByTradeIdAndVersion(
                tradeDto.getTradeId(), tradeDto.getVersion());
        
        Trade trade;
        if (existingTradeOpt.isPresent()) {
            // Update existing trade
            trade = existingTradeOpt.get();
            trade.setCounterPartyId(tradeDto.getCounterPartyId());
            trade.setBookId(tradeDto.getBookId());
            trade.setMaturityDate(tradeDto.getMaturityDate());
            trade.setLastUpdatedAt(LocalDateTime.now());
        } else {
            // Create new trade
            trade = Trade.builder()
                    .tradeId(tradeDto.getTradeId())
                    .version(tradeDto.getVersion())
                    .counterPartyId(tradeDto.getCounterPartyId())
                    .bookId(tradeDto.getBookId())
                    .maturityDate(tradeDto.getMaturityDate())
                    .expired("N")
                    .status("ACTIVE")
                    .build();
        }
        
        trade = tradeRepository.save(trade);
        
        // Create audit log with Trade entity (includes lastUpdatedAt)
        Map<String, Object> auditPayload = convertTradeToPayload(trade);
        TradeAuditLog auditLog = createAuditLog(tradeDto, source, VALIDATION_STATUS_ACCEPTED, 
                                                null, EVENT_TYPE_ACCEPTED, auditPayload);
        auditLogRepository.save(auditLog);
        
        // Update Redis cache - store both latest and all trades
        Map<String, Object> tradeMap = convertToMap(trade);
        cacheService.setLatestTrade(tradeDto.getTradeId(), tradeMap);
        cacheService.setTrade(tradeDto.getTradeId(), tradeDto.getVersion(), tradeMap);
        
        // Publish Kafka event (if Kafka is available)
        if (eventProducer != null) {
            Map<String, Object> payload = convertTradeToPayload(trade);
            eventProducer.sendTradeEvent(EVENT_TYPE_ACCEPTED, tradeDto.getTradeId(), 
                                        tradeDto.getVersion(), payload, null);
        } else {
            log.warn("Kafka not available - skipping event publication for tradeId={}", tradeDto.getTradeId());
        }
        
        return TradeSubmissionResponse.builder()
                .status(VALIDATION_STATUS_ACCEPTED)
                .eventType(EVENT_TYPE_ACCEPTED)
                .reason(null)
                .trade(tradeDto)
                .build();
    }
    
    private TradeSubmissionResponse handleRejection(TradeDto tradeDto, String source, String reason) {
        log.warn("Trade rejected: tradeId={}, version={}, reason={}", 
                tradeDto.getTradeId(), tradeDto.getVersion(), reason);
        
        // Create audit log (rejected trades are not stored in MySQL)
        Map<String, Object> auditPayload = convertTradeDtoToPayload(tradeDto);
        TradeAuditLog auditLog = createAuditLog(tradeDto, source, VALIDATION_STATUS_REJECTED, 
                                                reason, EVENT_TYPE_REJECTED, auditPayload);
        auditLogRepository.save(auditLog);
        
        // Publish Kafka event (if Kafka is available)
        if (eventProducer != null) {
            Map<String, Object> payload = convertTradeDtoToPayload(tradeDto);
            eventProducer.sendTradeEvent(EVENT_TYPE_REJECTED, tradeDto.getTradeId(), 
                                        tradeDto.getVersion(), payload, reason);
        } else {
            log.warn("Kafka not available - skipping event publication for tradeId={}", tradeDto.getTradeId());
        }
        
        return TradeSubmissionResponse.builder()
                .status(VALIDATION_STATUS_REJECTED)
                .eventType(EVENT_TYPE_REJECTED)
                .reason(reason)
                .trade(null)
                .build();
    }
    
    private TradeAuditLog createAuditLog(TradeDto tradeDto, String source, 
                                         String validationStatus, String rejectionReason, 
                                         String eventType, Map<String, Object> payload) {
        return TradeAuditLog.builder()
                .auditId(UUID.randomUUID().toString())
                .tradeId(tradeDto.getTradeId())
                .version(tradeDto.getVersion())
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .source(source)
                .validationStatus(validationStatus)
                .rejectionReason(rejectionReason)
                .eventType(eventType)
                .build();
    }
    
    private Map<String, Object> convertTradeDtoToPayload(TradeDto tradeDto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tradeId", tradeDto.getTradeId());
        payload.put("version", tradeDto.getVersion());
        payload.put("counterPartyId", tradeDto.getCounterPartyId());
        payload.put("bookId", tradeDto.getBookId());
        payload.put("maturityDate", tradeDto.getMaturityDate().toString());
        return payload;
    }
    
    private Map<String, Object> convertTradeToPayload(Trade trade) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tradeId", trade.getTradeId());
        payload.put("version", trade.getVersion());
        payload.put("counterPartyId", trade.getCounterPartyId());
        payload.put("bookId", trade.getBookId());
        payload.put("maturityDate", trade.getMaturityDate().toString());
        payload.put("createdDate", trade.getCreatedDate() != null ? trade.getCreatedDate().format(DATETIME_FORMATTER) : null);
        payload.put("lastUpdatedAt", trade.getLastUpdatedAt() != null ? trade.getLastUpdatedAt().format(DATETIME_FORMATTER) : null);
        payload.put("expired", trade.getExpired());
        payload.put("status", trade.getStatus());
        return payload;
    }
    
    private Map<String, Object> convertToMap(Trade trade) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", trade.getId());
        map.put("tradeId", trade.getTradeId());
        map.put("version", trade.getVersion());
        map.put("counterPartyId", trade.getCounterPartyId());
        map.put("bookId", trade.getBookId());
        map.put("maturityDate", trade.getMaturityDate().toString());
        map.put("createdDate", trade.getCreatedDate() != null ? trade.getCreatedDate().format(DATETIME_FORMATTER) : null);
        map.put("expired", trade.getExpired());
        map.put("status", trade.getStatus());
        map.put("lastUpdatedAt", trade.getLastUpdatedAt() != null ? trade.getLastUpdatedAt().format(DATETIME_FORMATTER) : null);
        return map;
    }
}

