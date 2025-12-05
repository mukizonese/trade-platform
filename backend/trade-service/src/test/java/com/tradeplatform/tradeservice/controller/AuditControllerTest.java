package com.tradeplatform.tradeservice.controller;

import com.tradeplatform.tradeservice.model.document.TradeAuditLog;
import com.tradeplatform.tradeservice.repository.TradeAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuditController.class,
        excludeAutoConfiguration = {
                MongoAutoConfiguration.class, 
                MongoDataAutoConfiguration.class,
                MongoRepositoriesAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.data.mongodb.repositories.enabled=false",
        "spring.jpa.repositories.enabled=false"
})
class AuditControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TradeAuditLogRepository auditLogRepository;
    
    @Test
    void testGetAuditLogs_ShouldReturnAuditLogs() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("tradeId", "T1");
        payload.put("version", 1);
        payload.put("bookId", "B1");
        payload.put("counterPartyId", "CP-1");
        payload.put("maturityDate", "2025-12-31");
        payload.put("expired", "N");
        
        TradeAuditLog auditLog = TradeAuditLog.builder()
                .auditId("audit-1")
                .tradeId("T1")
                .version(1)
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .source("UI_SIMULATOR")
                .validationStatus("ACCEPTED")
                .eventType("TRADE_ACCEPTED")
                .build();
        
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TradeAuditLog> auditLogPage = new PageImpl<>(List.of(auditLog), pageable, 1);
        
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(auditLogPage);
        
        // When & Then
        mockMvc.perform(get("/api/audit/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].auditId").value("audit-1"))
                .andExpect(jsonPath("$.content[0].tradeId").value("T1"))
                .andExpect(jsonPath("$.content[0].bookId").value("B1"))
                .andExpect(jsonPath("$.content[0].validationStatus").value("ACCEPTED"));
    }
    
    @Test
    void testGetAuditLogs_EmptyResult_ShouldReturnEmptyPage() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TradeAuditLog> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
        
        // When & Then
        mockMvc.perform(get("/api/audit/trades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
