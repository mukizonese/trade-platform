package com.tradeplatform.tradeservice.integration;

import com.tradeplatform.tradeservice.dto.TradeDto;
import com.tradeplatform.tradeservice.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeservice.model.entity.Trade;
import com.tradeplatform.tradeservice.repository.TradeRepository;
import com.tradeplatform.tradeservice.service.TradeCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeSubmissionIntegrationTest {
    
    @Mock
    private TradeRepository tradeRepository;
    
    @Mock
    private com.tradeplatform.tradeservice.repository.TradeAuditLogRepository auditLogRepository;
    
    @Mock
    private com.tradeplatform.tradeservice.service.CacheService cacheService;
    
    @Mock
    private com.tradeplatform.tradeservice.kafka.TradeEventProducer eventProducer;
    
    @InjectMocks
    private TradeCommandService commandService;
    
    @BeforeEach
    void setUp() {
        reset(tradeRepository, auditLogRepository, cacheService, eventProducer);
    }
    
    @Test
    void testSubmitTrade_NewTrade_ShouldPersistInDatabase() {
        // Given
        TradeDto tradeDto = TradeDto.builder()
                .tradeId("T-INT-1")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        when(tradeRepository.findMaxVersionByTradeId("T-INT-1")).thenReturn(Optional.empty());
        when(tradeRepository.findByTradeIdAndVersion("T-INT-1", 1)).thenReturn(Optional.empty());
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        TradeSubmissionResponse response = commandService.submitTrade(tradeDto, "TEST");
        
        // Then
        assertEquals("ACCEPTED", response.getStatus());
        
        // Verify trade was saved
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository, times(1)).save(tradeCaptor.capture());
        
        Trade savedTrade = tradeCaptor.getValue();
        assertEquals("T-INT-1", savedTrade.getTradeId());
        assertEquals(1, savedTrade.getVersion());
        assertEquals("CP-1", savedTrade.getCounterPartyId());
        assertEquals("B1", savedTrade.getBookId());
    }
    
    @Test
    void testSubmitTrade_LowerVersion_ShouldReject() {
        // Given: Submit version 2 first
        TradeDto v2 = TradeDto.builder()
                .tradeId("T-INT-2")
                .version(2)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        when(tradeRepository.findMaxVersionByTradeId("T-INT-2")).thenReturn(Optional.empty());
        when(tradeRepository.findByTradeIdAndVersion("T-INT-2", 2)).thenReturn(Optional.empty());
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        commandService.submitTrade(v2, "TEST");
        
        // When: Try to submit version 1 (lower)
        TradeDto v1 = TradeDto.builder()
                .tradeId("T-INT-2")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        when(tradeRepository.findMaxVersionByTradeId("T-INT-2")).thenReturn(Optional.of(2));
        
        TradeSubmissionResponse response = commandService.submitTrade(v1, "TEST");
        
        // Then: Should be rejected
        assertEquals("REJECTED", response.getStatus());
        assertEquals("LOWER_VERSION", response.getReason());
        
        // Verify version 1 was NOT saved
        verify(tradeRepository, never()).save(argThat(trade -> 
            trade.getTradeId().equals("T-INT-2") && trade.getVersion() == 1));
    }
}
