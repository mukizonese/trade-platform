package com.tradeplatform.tradeservice.controller;

import com.tradeplatform.tradeservice.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trades/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {
    
    private final CacheService cacheService;
    
    /**
     * Get all trades from Redis cache
     * Returns all cached trades (all versions of all trades)
     * Sorted by LastUpdatedAt descending (most recently updated first)
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTradesFromCache() {
        //log.info("Retrieving all trades from cache");
        List<Map<String, Object>> trades = cacheService.getAllTrades();
        return ResponseEntity.ok(trades);
    }
    
    /**
     * Get all versions of a specific trade from Redis cache
     * Sorted by LastUpdatedAt descending (most recently updated first)
     */
    @GetMapping("/{tradeId}")
    public ResponseEntity<List<Map<String, Object>>> getTradesFromCacheByTradeId(
            @PathVariable String tradeId) {
        //log.info("Retrieving trades for tradeId: {} from cache", tradeId);
        List<Map<String, Object>> trades = cacheService.getTradesByTradeId(tradeId);
        if (trades.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trades);
    }
}

