package com.tradeplatform.tradeservice.controller;

import com.tradeplatform.tradeservice.dto.AuditDto;
import com.tradeplatform.tradeservice.model.document.TradeAuditLog;
import com.tradeplatform.tradeservice.repository.TradeAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {
    
    private final TradeAuditLogRepository auditLogRepository;
    
    @GetMapping("/trades")
    public ResponseEntity<Page<AuditDto>> getAuditLogs(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        
        log.info("Fetching audit logs: page={}, size={}", page, size);
        
        // Sort by timestamp descending 
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<TradeAuditLog> auditLogs = auditLogRepository.findAll(pageable);
        
        Page<AuditDto> auditDtos = auditLogs.map(this::convertToDto);
        
        return ResponseEntity.ok(auditDtos);
    }
    
    private AuditDto convertToDto(TradeAuditLog auditLog) {
        Map<String, Object> payload = auditLog.getPayload();
        
        // Extract fields from payload
        String bookId = extractString(payload, "bookId");
        String counterPartyId = extractString(payload, "counterPartyId");
        String maturityDate = extractString(payload, "maturityDate");
        String expired = extractString(payload, "expired");
        String createdDate = extractString(payload, "createdDate");
        String lastUpdatedAt = extractString(payload, "lastUpdatedAt");
        
        return AuditDto.builder()
                .auditId(auditLog.getAuditId())
                .tradeId(auditLog.getTradeId())
                .version(auditLog.getVersion())
                .timestamp(auditLog.getTimestamp())
                .bookId(bookId)
                .counterPartyId(counterPartyId)
                .maturityDate(maturityDate)
                .expired(expired)
                .createdDate(createdDate)
                .lastUpdatedAt(lastUpdatedAt)
                .source(auditLog.getSource())
                .validationStatus(auditLog.getValidationStatus())
                .eventType(auditLog.getEventType())
                .build();
    }
    
    private String extractString(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}

