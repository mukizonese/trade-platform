package com.tradeplatform.tradeingress.controller;

import com.tradeplatform.common.dto.AuditDto;
import com.tradeplatform.tradeingress.client.TradeServiceClient;
import com.tradeplatform.tradeingress.config.ApiPathsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuditController {
    
    private final TradeServiceClient tradeServiceClient;
    private final ApiPathsConfig apiPathsConfig;
    
    @GetMapping("${api.paths.audit:/api/audit}/trades")
    public ResponseEntity<Page<AuditDto>> getAuditLogs(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        
        log.info("Fetching audit logs: page={}, size={}", page, size);
        
        try {
            Page<AuditDto> auditLogs = tradeServiceClient.getAuditLogs(page, size)
                    .block();
            
            if (auditLogs == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

