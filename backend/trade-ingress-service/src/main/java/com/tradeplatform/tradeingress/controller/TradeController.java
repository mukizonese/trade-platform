package com.tradeplatform.tradeingress.controller;

import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeQueryResponse;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeingress.client.TradeServiceClient;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
        } catch (CallNotPermittedException ex) {
            TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                    .status("REJECTED")
                    .eventType("TRADE_REJECTED")
                    .reason("CIRCUIT_BREAKER_OPEN")
                    .message("Circuit breaker is OPEN. Trade service temporarily unavailable.")
                    .trade(tradeDto)
                    .build();
    
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response); // 503
        } catch (WebClientResponseException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            String reason = status != null && status.is4xxClientError() ? "CLIENT_ERROR" : "SERVICE_ERROR";
            String message = e.getMessage();
            
            TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                    .status("REJECTED")
                    .eventType("TRADE_REJECTED")
                    .reason(reason)
                    .message(message != null ? message : "Error occurred while processing trade.")
                    .trade(tradeDto)
                    .build();
            
            HttpStatus responseStatus = status != null && status.is4xxClientError() 
                    ? status 
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(responseStatus).body(response);
        } catch (Exception e) {
            TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                .status("REJECTED")
                .eventType("TRADE_REJECTED")
                .reason("GENERIC_ERROR")
                .message("Unexpected error occurred while submitting trade.")
                .trade(tradeDto)
                .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
        }
    }
    
    public ResponseEntity<TradeSubmissionResponse> submitTradeFallback(
            //TradeDto tradeDto, String source) {
            TradeDto tradeDto, String source, Throwable ex) {
        log.warn("Rate limit exceeded for tradeId={}", tradeDto.getTradeId());
        TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                .status("REJECTED")
                .eventType("TRADE_REJECTED")
                .reason("RATE_LIMIT_EXCEEDED")
                .message("Service rate limit exceeded. Please try again later.")
                .trade(tradeDto)
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response); //429
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

