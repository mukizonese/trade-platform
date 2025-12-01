package com.tradeplatform.tradeservice.controller;

import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeQueryResponse;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import com.tradeplatform.tradeservice.service.TradeCommandService;
import com.tradeplatform.tradeservice.service.TradeQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Slf4j
public class TradeController {
    
    private final TradeCommandService commandService;
    private final TradeQueryService queryService;
    
    @PostMapping
    public ResponseEntity<TradeSubmissionResponse> submitTrade(
            @Valid @RequestBody TradeDto tradeDto,
            @RequestParam(defaultValue = "UI_SIMULATOR") String source) {
        
        log.info("Received trade submission: tradeId={}, version={}", 
                tradeDto.getTradeId(), tradeDto.getVersion());
        
        TradeSubmissionResponse response = commandService.submitTrade(tradeDto, source);
        
        HttpStatus status = "ACCEPTED".equals(response.getStatus()) 
                ? HttpStatus.OK 
                : HttpStatus.BAD_REQUEST;
        
        return ResponseEntity.status(status).body(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<TradeQueryResponse>> getTrades(
            @RequestParam(required = false, defaultValue = "true") Boolean latestOnly,
            @RequestParam(required = false) String tradeId,
            @RequestParam(required = false) String bookId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Page<TradeQueryResponse> trades = queryService.getTrades(
                latestOnly, tradeId, bookId, status, page, size);
        
        return ResponseEntity.ok(trades);
    }
    
    @GetMapping("/{tradeId}")
    public ResponseEntity<List<TradeQueryResponse>> getTradeById(
            @PathVariable String tradeId,
            @RequestParam(required = false, defaultValue = "false") Boolean latestOnly) {
        
        List<TradeQueryResponse> trades = queryService.getTradeById(tradeId, latestOnly);
        
        if (trades.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(trades);
    }
}

