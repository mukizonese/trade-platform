package com.tradeplatform.tradeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeSubmissionResponse {
    
    private String status; // "ACCEPTED" | "REJECTED"
    
    private String eventType; // "TRADE_ACCEPTED" | "TRADE_REJECTED"
    
    private String reason; // "LOWER_VERSION" | "PAST_MATURITY" | null
    
    private TradeDto trade; // Trade data if accepted
}

