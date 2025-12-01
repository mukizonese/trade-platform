package com.tradeplatform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDto {
    
    private String auditId;
    
    private String tradeId;
    
    private Integer version;
    
    private LocalDateTime timestamp;
    
    private String bookId;
    
    private String counterPartyId;
    
    private String maturityDate;
    
    private String expired;
    
    private String createdDate;
    
    private String lastUpdatedAt;
    
    private String source;
    
    private String validationStatus;
    
    private String eventType;
}

