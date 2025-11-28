package com.tradeplatform.tradeservice.repository;

import com.tradeplatform.tradeservice.model.document.TradeAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeAuditLogRepository extends MongoRepository<TradeAuditLog, String> {
    
    List<TradeAuditLog> findByTradeId(String tradeId);
    
    Page<TradeAuditLog> findByTradeId(String tradeId, Pageable pageable);
    
    Page<TradeAuditLog> findByEventType(String eventType, Pageable pageable);
    
    Page<TradeAuditLog> findByValidationStatus(String validationStatus, Pageable pageable);
    
    Page<TradeAuditLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}

