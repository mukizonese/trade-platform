package com.tradeplatform.tradeservice.service;

import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeservice.kafka.TradeEventProducer;
import com.tradeplatform.tradeservice.model.document.TradeAuditLog;
import com.tradeplatform.tradeservice.model.entity.Trade;
import com.tradeplatform.tradeservice.repository.TradeAuditLogRepository;
import com.tradeplatform.tradeservice.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeCommandServiceTest {
    
    @Mock
    private TradeRepository tradeRepository;
    
    @Mock
    private TradeAuditLogRepository auditLogRepository;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private TradeEventProducer eventProducer;
    
    @InjectMocks
    private TradeCommandService commandService;
    
    private TradeDto validTradeDto;
    
    @BeforeEach
    void setUp() {
        validTradeDto = TradeDto.builder()
                .tradeId("T1")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
    }
    
    @Test
    void testSubmitTrade_NewTrade_ShouldAccept() {
        // Given: No existing trade
        when(tradeRepository.findMaxVersionByTradeId("T1")).thenReturn(Optional.empty());
        when(tradeRepository.findByTradeIdAndVersion("T1", 1)).thenReturn(Optional.empty());
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeSubmissionResponse response = commandService.submitTrade(validTradeDto, "UI_SIMULATOR");
        
        // Then
        assertEquals("ACCEPTED", response.getStatus());
        assertEquals("TRADE_ACCEPTED", response.getEventType());
        assertNotNull(response.getTrade());
        
        // Verify MySQL save
        verify(tradeRepository, times(1)).save(any(Trade.class));
        
        // Verify audit log
        verify(auditLogRepository, times(1)).save(any(TradeAuditLog.class));
    }
    
    @Test
    void testSubmitTrade_LowerVersion_ShouldReject() {
        // Given: Existing trade with version 2
        when(tradeRepository.findMaxVersionByTradeId("T1")).thenReturn(Optional.of(2));
        
        // When
        TradeSubmissionResponse response = commandService.submitTrade(validTradeDto, "UI_SIMULATOR");
        
        // Then
        assertEquals("REJECTED", response.getStatus());
        assertEquals("TRADE_REJECTED", response.getEventType());
        assertEquals("LOWER_VERSION", response.getReason());
        
        // Verify NO MySQL save
        verify(tradeRepository, never()).save(any(Trade.class));
        
        // Verify audit log (rejected trades are still audited)
        verify(auditLogRepository, times(1)).save(any(TradeAuditLog.class));
    }
}
