package com.tradeplatform.tradeservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String LATEST_TRADE_KEY_PREFIX = "trade:latest:";
    private static final String ALL_TRADE_KEY_PREFIX = "trade:";
    //private static final Duration DEFAULT_TTL = Duration.ofSeconds(300); // 5 minutes
    private static final Duration DEFAULT_TTL = Duration.ofDays(30); // 30 days
    
    public <T> void setLatestTrade(String tradeId, T trade) {
        try {
            String key = LATEST_TRADE_KEY_PREFIX + tradeId;
            String value = objectMapper.writeValueAsString(trade);
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
            log.debug("Cached latest trade for tradeId: {}", tradeId);
        } catch (JsonProcessingException e) {
            log.error("Error caching latest trade for tradeId: {}", tradeId, e);
        }
    }
    
    public <T> Optional<T> getLatestTrade(String tradeId, Class<T> clazz) {
        try {
            String key = LATEST_TRADE_KEY_PREFIX + tradeId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                T trade = objectMapper.readValue(value, clazz);
                //log.debug("Retrieved cached latest trade for tradeId: {}", tradeId);
                return Optional.of(trade);
            }
        } catch (JsonProcessingException e) {
            log.error("Error retrieving cached latest trade for tradeId: {}", tradeId, e);
        }
        return Optional.empty();
    }
    
    public void invalidateLatestTrade(String tradeId) {
        String key = LATEST_TRADE_KEY_PREFIX + tradeId;
        redisTemplate.delete(key);
        log.debug("Invalidated cached latest trade for tradeId: {}", tradeId);
    }
    
    public void set(String key, Object value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
        } catch (JsonProcessingException e) {
            log.error("Error caching value for key: {}", key, e);
        }
    }
    
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return Optional.of(objectMapper.readValue(value, clazz));
            }
        } catch (JsonProcessingException e) {
            log.error("Error retrieving cached value for key: {}", key, e);
        }
        return Optional.empty();
    }
    
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    /**
     * Cache a trade with its version (stores all trades, not just latest)
     * Key pattern: trade:<tradeId>:<version>
     */
    public <T> void setTrade(String tradeId, Integer version, T trade) {
        try {
            String key = ALL_TRADE_KEY_PREFIX + tradeId + ":" + version;
            String value = objectMapper.writeValueAsString(trade);
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
            log.debug("Cached trade for tradeId: {}, version: {}", tradeId, version);
        } catch (JsonProcessingException e) {
            log.error("Error caching trade for tradeId: {}, version: {}", tradeId, version, e);
        }
    }
    
    /**
     * Get a specific trade by tradeId and version from cache
     */
    public <T> Optional<T> getTrade(String tradeId, Integer version, Class<T> clazz) {
        try {
            String key = ALL_TRADE_KEY_PREFIX + tradeId + ":" + version;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                T trade = objectMapper.readValue(value, clazz);
                log.debug("Retrieved cached trade for tradeId: {}, version: {}", tradeId, version);
                return Optional.of(trade);
            }
        } catch (JsonProcessingException e) {
            log.error("Error retrieving cached trade for tradeId: {}, version: {}", tradeId, version, e);
        }
        return Optional.empty();
    }
    
    /**
     * Get all trades from cache (all versions of all trades)
     * Returns sorted by LastUpdatedAt descending (most recently updated first)
     * Scans for keys matching pattern: trade:*
     */
    public List<Map<String, Object>> getAllTrades() {
        List<Map<String, Object>> trades = new ArrayList<>();
        try {
            Set<String> keys = redisTemplate.keys(ALL_TRADE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    // Skip latest trade keys
                    if (!key.startsWith(LATEST_TRADE_KEY_PREFIX)) {
                        String value = redisTemplate.opsForValue().get(key);
                        if (value != null) {
                            Map<String, Object> trade = objectMapper.readValue(value, 
                                    new TypeReference<Map<String, Object>>() {});
                            trades.add(trade);
                        }
                    }
                }
                //log.debug("Retrieved {} trades from cache", trades.size());
            }
        } catch (JsonProcessingException e) {
            log.error("Error retrieving all trades from cache", e);
        }
        return sortByLastUpdatedAtDescending(trades);
    }
    
    /**
     * Get all versions of a specific trade from cache
     * Returns sorted by LastUpdatedAt descending (most recently updated first)
     * Scans for keys matching pattern: trade:<tradeId>:*
     */
    public List<Map<String, Object>> getTradesByTradeId(String tradeId) {
        List<Map<String, Object>> trades = new ArrayList<>();
        try {
            String pattern = ALL_TRADE_KEY_PREFIX + tradeId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        Map<String, Object> trade = objectMapper.readValue(value, 
                                new TypeReference<Map<String, Object>>() {});
                        trades.add(trade);
                    }
                }
                log.debug("Retrieved {} versions of trade {} from cache", trades.size(), tradeId);
            }
        } catch (JsonProcessingException e) {
            log.error("Error retrieving trades for tradeId: {} from cache", tradeId, e);
        }
        return sortByLastUpdatedAtDescending(trades);
    }
    
    /**
     * Sort trades by LastUpdatedAt in descending order (most recently updated first)
     * Handles both string format (from cache) and LocalDateTime format
     */
    private List<Map<String, Object>> sortByLastUpdatedAtDescending(List<Map<String, Object>> trades) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return trades.stream()
                .sorted((t1, t2) -> {

                    
                    // First try epoch millis
                    Long e1 = (Long) t1.get("lastUpdatedAtEpoch");
                    Long e2 = (Long) t2.get("lastUpdatedAtEpoch");

                    if (e1 != null && e2 != null) {
                        return e2.compareTo(e1);
                    }
                    
                    String lastUpdated1 = (String) t1.get("lastUpdatedAt");
                    String lastUpdated2 = (String) t2.get("lastUpdatedAt");
                    
                    // Fallback to the old string parsing for backward compatibility
                    if (lastUpdated1 == null && lastUpdated2 == null) {
                        return 0;
                    }
                    if (lastUpdated1 == null) {
                        return 1; // null goes after
                    }
                    if (lastUpdated2 == null) {
                        return -1; // null goes after
                    }
                    
                    try {
                        LocalDateTime date1 = LocalDateTime.parse(lastUpdated1, formatter);
                        LocalDateTime date2 = LocalDateTime.parse(lastUpdated2, formatter);
                        // Descending order: most recent first
                        return date2.compareTo(date1);
                    } catch (Exception e) {
                        log.warn("Error parsing lastUpdatedAt for sorting: {}", e.getMessage());
                        // If parsing fails, maintain original order
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Invalidate a specific trade version from cache
     */
    public void invalidateTrade(String tradeId, Integer version) {
        String key = ALL_TRADE_KEY_PREFIX + tradeId + ":" + version;
        redisTemplate.delete(key);
        log.debug("Invalidated cached trade for tradeId: {}, version: {}", tradeId, version);
    }
    
    /**
     * Invalidate all versions of a trade from cache
     */
    public void invalidateAllTradesByTradeId(String tradeId) {
        String pattern = ALL_TRADE_KEY_PREFIX + tradeId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Invalidated {} cached trades for tradeId: {}", keys.size(), tradeId);
        }
    }
}

