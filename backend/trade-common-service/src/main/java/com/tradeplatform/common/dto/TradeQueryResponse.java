package com.tradeplatform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeQueryResponse {
    
    private Long id;
    
    private String tradeId;
    
    private Integer version;
    
    private String counterPartyId;
    
    private String bookId;
    
    private LocalDate maturityDate;
    
    private LocalDateTime createdDate;
    
    private String expired; // "Y" | "N"
    
    private String status; // "ACTIVE" | "EXPIRED" | "CANCELLED"
    
    private LocalDateTime lastUpdatedAt;
}

