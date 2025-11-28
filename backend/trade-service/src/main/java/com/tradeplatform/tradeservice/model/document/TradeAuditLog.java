package com.tradeplatform.tradeservice.model.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "trade_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeAuditLog {
    
    @Id
    private String auditId;
    
    private String tradeId;
    
    private Integer version;
    
    @CreatedDate
    private LocalDateTime timestamp;
    
    private Map<String, Object> payload;
    
    private String source; // "UI_SIMULATOR", "BATCH", "EXTERNAL", etc.
    
    private String validationStatus; // "ACCEPTED" | "REJECTED"
    
    private String rejectionReason; // "LOWER_VERSION" | "PAST_MATURITY" | null
    
    private String eventType; // "TRADE_ACCEPTED" | "TRADE_REJECTED" | "TRADE_EXPIRED"
}

