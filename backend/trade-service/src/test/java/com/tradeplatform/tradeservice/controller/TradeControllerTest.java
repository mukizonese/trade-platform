package com.tradeplatform.tradeservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeservice.service.TradeCommandService;
import com.tradeplatform.tradeservice.service.TradeQueryService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TradeController.class, 
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
class TradeControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TradeCommandService commandService;
    
    @MockBean
    private TradeQueryService queryService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testSubmitTrade_ValidTrade_ShouldReturnAccepted() throws Exception {
        // Given
        TradeDto tradeDto = TradeDto.builder()
                .tradeId("T1")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                .status("ACCEPTED")
                .eventType("TRADE_ACCEPTED")
                .trade(tradeDto)
                .build();
        
        when(commandService.submitTrade(any(TradeDto.class), eq("UI_SIMULATOR")))
                .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.eventType").value("TRADE_ACCEPTED"));
    }
    
    @Test
    void testSubmitTrade_RejectedTrade_ShouldReturnBadRequest() throws Exception {
        // Given
        TradeDto tradeDto = TradeDto.builder()
                .tradeId("T1")
                .version(1)
                .counterPartyId("CP-1")
                .bookId("B1")
                .maturityDate(LocalDate.now().plusDays(30))
                .build();
        
        TradeSubmissionResponse response = TradeSubmissionResponse.builder()
                .status("REJECTED")
                .eventType("TRADE_REJECTED")
                .reason("LOWER_VERSION")
                .build();
        
        when(commandService.submitTrade(any(TradeDto.class), eq("UI_SIMULATOR")))
                .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("LOWER_VERSION"));
    }
}
