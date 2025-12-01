package com.tradeplatform.tradeingress.controller;

import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeQueryResponse;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeingress.client.TradeServiceClient;
import com.tradeplatform.tradeingress.config.ApiPathsConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TradeController {
    
    private final TradeServiceClient tradeServiceClient;
    private final ApiPathsConfig apiPathsConfig;
    
    @PostMapping("${api.paths.trades:/api/trades}")
    public ResponseEntity<TradeSubmissionResponse> submitTrade(
            @Valid @RequestBody TradeDto tradeDto,
            @RequestParam(defaultValue = "UI_SIMULATOR") String source) {
        
        log.info("Received trade submission: tradeId={}, version={}", 
                tradeDto.getTradeId(), tradeDto.getVersion());
        
        try {
            TradeSubmissionResponse response = tradeServiceClient.submitTrade(tradeDto, source)
                    .block();
            
            if (response == null) {
                log.error("Null response from trade-service for tradeId={}", tradeDto.getTradeId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            HttpStatus status = "ACCEPTED".equals(response.getStatus()) 
                    ? HttpStatus.OK 
                    : HttpStatus.BAD_REQUEST;
            
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("${api.paths.trades:/api/trades}")
    public ResponseEntity<Page<TradeQueryResponse>> getTrades(
            @RequestParam(required = false, defaultValue = "true") Boolean latestOnly,
            @RequestParam(required = false) String tradeId,
            @RequestParam(required = false) String bookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        try {
            Page<TradeQueryResponse> trades = tradeServiceClient.getTrades(
                    latestOnly, tradeId, bookId, status, page, size)
                    .block();
            
            if (trades == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("${api.paths.trades:/api/trades}/{tradeId}")
    public ResponseEntity<List<TradeQueryResponse>> getTradeById(
            @PathVariable String tradeId,
            @RequestParam(required = false, defaultValue = "false") Boolean latestOnly) {
        
        try {
            List<TradeQueryResponse> trades = tradeServiceClient.getTradeById(tradeId, latestOnly)
                    .block();
            
            if (trades == null || trades.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

