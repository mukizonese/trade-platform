package com.tradeplatform.tradeservice.controller;

import com.tradeplatform.tradeservice.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CacheController.class,
        excludeAutoConfiguration = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ActiveProfiles("test")
class CacheControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CacheService cacheService;
    
    @Test
    void testGetAllTradesFromCache_ShouldReturnSortedTrades() throws Exception {
        // Given: Create trades with different LastUpdatedAt values
        // Service should return them sorted by LastUpdatedAt descending (most recent first)
        Map<String, Object> trade1 = createTradeMap("T1", 1, "2025-11-28 10:00:00");
        Map<String, Object> trade2 = createTradeMap("T2", 1, "2025-11-28 11:00:00");
        Map<String, Object> trade3 = createTradeMap("T3", 1, "2025-11-28 09:00:00");
        
        // Service returns sorted: T2 (11:00) -> T1 (10:00) -> T3 (09:00)
        List<Map<String, Object>> trades = Arrays.asList(trade2, trade1, trade3);
        
        when(cacheService.getAllTrades()).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tradeId").value("T2")) // Most recent first
                .andExpect(jsonPath("$[0].lastUpdatedAt").value("2025-11-28 11:00:00"))
                .andExpect(jsonPath("$[1].tradeId").value("T1"))
                .andExpect(jsonPath("$[1].lastUpdatedAt").value("2025-11-28 10:00:00"))
                .andExpect(jsonPath("$[2].tradeId").value("T3")) // Oldest last
                .andExpect(jsonPath("$[2].lastUpdatedAt").value("2025-11-28 09:00:00"));
    }
    
    @Test
    void testGetAllTradesFromCache_EmptyCache_ShouldReturnEmptyList() throws Exception {
        // Given
        when(cacheService.getAllTrades()).thenReturn(Collections.emptyList());
        
        // When & Then
        mockMvc.perform(get("/api/trades/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
    
    @Test
    void testGetTradesFromCacheByTradeId_ShouldReturnSortedTrades() throws Exception {
        // Given: Multiple versions of the same trade
        // Service should return them sorted by LastUpdatedAt descending (most recent first)
        Map<String, Object> trade1 = createTradeMap("T1", 1, "2025-11-28 10:00:00");
        Map<String, Object> trade2 = createTradeMap("T1", 2, "2025-11-28 11:00:00");
        Map<String, Object> trade3 = createTradeMap("T1", 3, "2025-11-28 09:00:00");
        
        // Service returns sorted: version 2 (11:00) -> version 1 (10:00) -> version 3 (09:00)
        List<Map<String, Object>> trades = Arrays.asList(trade2, trade1, trade3);
        
        when(cacheService.getTradesByTradeId("T1")).thenReturn(trades);
        
        // When & Then
        mockMvc.perform(get("/api/trades/cache/T1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].version").value(2)) // Most recent first
                .andExpect(jsonPath("$[0].lastUpdatedAt").value("2025-11-28 11:00:00"))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].lastUpdatedAt").value("2025-11-28 10:00:00"))
                .andExpect(jsonPath("$[2].version").value(3)) // Oldest last
                .andExpect(jsonPath("$[2].lastUpdatedAt").value("2025-11-28 09:00:00"));
    }
    
    @Test
    void testGetTradesFromCacheByTradeId_NotFound_ShouldReturn404() throws Exception {
        // Given
        when(cacheService.getTradesByTradeId("T999")).thenReturn(Collections.emptyList());
        
        // When & Then
        mockMvc.perform(get("/api/trades/cache/T999"))
                .andExpect(status().isNotFound());
    }
    
    private Map<String, Object> createTradeMap(String tradeId, Integer version, String lastUpdatedAt) {
        Map<String, Object> trade = new HashMap<>();
        trade.put("tradeId", tradeId);
        trade.put("version", version);
        trade.put("counterPartyId", "CP-1");
        trade.put("bookId", "B1");
        trade.put("maturityDate", "2025-12-31");
        trade.put("createdDate", "2025-11-28 08:00:00");
        trade.put("lastUpdatedAt", lastUpdatedAt);
        trade.put("expired", "N");
        trade.put("status", "ACTIVE");
        return trade;
    }
}

