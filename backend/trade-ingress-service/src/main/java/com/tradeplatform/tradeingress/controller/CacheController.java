package com.tradeplatform.tradeingress.controller;

import com.tradeplatform.tradeingress.client.TradeServiceClient;
import com.tradeplatform.tradeingress.config.ApiPathsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CacheController {
    
    private final TradeServiceClient tradeServiceClient;
    private final ApiPathsConfig apiPathsConfig;
    
    /**
     * Get all trades from Redis cache
     * Returns all cached trades (all versions of all trades)
     * Sorted by LastUpdatedAt descending (most recently updated first)
     */
    @GetMapping("${api.paths.cache:/api/trades/cache}")
    public ResponseEntity<List<Map<String, Object>>> getAllTradesFromCache() {
        try {
            List<Map<String, Object>> trades = tradeServiceClient.getAllTradesFromCache()
                    .block();
            
            if (trades == null) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all versions of a specific trade from Redis cache
     * Sorted by LastUpdatedAt descending (most recently updated first)
     */
    @GetMapping("${api.paths.cache:/api/trades/cache}/{tradeId}")
    public ResponseEntity<List<Map<String, Object>>> getTradesFromCacheByTradeId(
            @PathVariable String tradeId) {
        try {
            List<Map<String, Object>> trades = tradeServiceClient.getTradesFromCacheByTradeId(tradeId)
                    .block();
            
            if (trades == null || trades.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(trades);
        } catch (Exception e) {
            log.error("Error calling trade-service: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

