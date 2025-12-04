package com.tradeplatform.tradeingress.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeQueryResponse;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeingress.client.TradeServiceClient;
import com.tradeplatform.tradeingress.config.ApiPathsConfig;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TradeController {
    
    private final TradeServiceClient tradeServiceClient;
    private final ApiPathsConfig apiPathsConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostMapping("${api.paths.trades:/api/trades}")
    @RateLimiter(name = "submitTrade", fallbackMethod = "submitTradeFallback")
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
            
            return ResponseEntity.ok(response);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                try {
                    String responseBody = e.getResponseBodyAsString();
                    TradeSubmissionResponse errorResponse = objectMapper.readValue(
                            responseBody, TradeSubmissionResponse.class);
                    return ResponseEntity.ok(errorResponse);
                } catch (Exception parseException) {
                    return ResponseEntity.status(e.getStatusCode()).build();
                }
            }
            log.error("Error calling trade-service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    public ResponseEntity<TradeSubmissionResponse> submitTradeFallback(
            TradeDto tradeDto, String source, Exception ex) {
        log.warn("Rate limit exceeded at service layer for tradeId={}: {}", 
                tradeDto.getTradeId(), ex.getMessage());
        
        TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                .status("REJECTED")
                .eventType("TRADE_REJECTED")
                .reason("RATE_LIMIT_EXCEEDED")
                .message("Service rate limit exceeded. Please try again later.")
                .trade(tradeDto)
                .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
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

