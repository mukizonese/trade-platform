package com.tradeplatform.tradeservice.integration;

import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.tradeservice.model.document.TradeAuditLog;
import com.tradeplatform.tradeservice.repository.TradeAuditLogRepository;
import com.tradeplatform.tradeservice.service.TradeCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditIntegrationTest {
    
    @Mock
    private TradeAuditLogRepository auditLogRepository;
    
    @Mock
    private com.tradeplatform.tradeservice.repository.TradeRepository tradeRepository;
    
    @Mock
    private com.tradeplatform.tradeservice.service.CacheService cacheService;
    
    @Mock
    private com.tradeplatform.tradeservice.kafka.TradeEventProducer eventProducer;
    
    @InjectMocks
    private TradeCommandService commandService;
    
    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(auditLogRepository, tradeRepository, cacheService, eventProducer);
    }
    
    @Test
    void testSubmitTrade_AcceptedTrade_ShouldCreateAuditLog() {
        // Given
        TradeDto tradeDto = TradeDto.builder()
                .tradeId("T-AUDIT-1")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        when(tradeRepository.findMaxVersionByTradeId("T-AUDIT-1")).thenReturn(java.util.Optional.empty());
        when(tradeRepository.findByTradeIdAndVersion("T-AUDIT-1", 1)).thenReturn(java.util.Optional.empty());
        when(tradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(TradeAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        commandService.submitTrade(tradeDto, "TEST_SOURCE");
        
        // Then: Verify audit log was created
        ArgumentCaptor<TradeAuditLog> auditLogCaptor = ArgumentCaptor.forClass(TradeAuditLog.class);
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        TradeAuditLog auditLog = auditLogCaptor.getValue();
        assertEquals("T-AUDIT-1", auditLog.getTradeId());
        assertEquals(1, auditLog.getVersion());
        assertEquals("TEST_SOURCE", auditLog.getSource());
        assertEquals("ACCEPTED", auditLog.getValidationStatus());
        assertEquals("TRADE_ACCEPTED", auditLog.getEventType());
        assertNotNull(auditLog.getPayload());
        
        // Verify payload contains trade data
        Map<String, Object> payload = auditLog.getPayload();
        assertEquals("T-AUDIT-1", payload.get("tradeId"));
        assertEquals(1, payload.get("version"));
    }
    
    @Test
    void testSubmitTrade_RejectedTrade_ShouldCreateRejectedAuditLog() {
        // Given: Submit lower version (should be rejected)
        TradeDto tradeDto = TradeDto.builder()
                .tradeId("T-AUDIT-2")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        when(tradeRepository.findMaxVersionByTradeId("T-AUDIT-2")).thenReturn(java.util.Optional.of(2));
        when(auditLogRepository.save(any(TradeAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        commandService.submitTrade(tradeDto, "TEST_SOURCE");
        
        // Then: Verify rejected audit log was created
        ArgumentCaptor<TradeAuditLog> auditLogCaptor = ArgumentCaptor.forClass(TradeAuditLog.class);
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        TradeAuditLog auditLog = auditLogCaptor.getValue();
        assertEquals("T-AUDIT-2", auditLog.getTradeId());
        assertEquals("REJECTED", auditLog.getValidationStatus());
        assertEquals("TRADE_REJECTED", auditLog.getEventType());
        assertEquals("LOWER_VERSION", auditLog.getRejectionReason());
    }
}
