package com.tradeplatform.tradeservice.service;

import com.tradeplatform.common.dto.TradeQueryResponse;
import com.tradeplatform.tradeservice.model.entity.Trade;
import com.tradeplatform.tradeservice.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeQueryService {
    
    private final TradeRepository tradeRepository;
    
    public Page<TradeQueryResponse> getTrades(Boolean latestOnly, String tradeId, 
                                               String bookId, String status, 
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        if (tradeId != null && !tradeId.isEmpty()) {
            if (Boolean.TRUE.equals(latestOnly)) {
                List<TradeQueryResponse> latestTrade = tradeRepository.findLatestByTradeId(tradeId)
                        .map(t -> List.of(convertToResponse(t)))
                        .orElse(List.of());
                return new org.springframework.data.domain.PageImpl<>(
                        latestTrade, pageable, latestTrade.size());
            } else {
                List<Trade> trades = tradeRepository.findByTradeId(tradeId);
                return convertToPage(trades, pageable);
            }
        }
        
        Page<Trade> trades = tradeRepository.findAll(pageable);
        
        if (Boolean.TRUE.equals(latestOnly)) {
            // Filter to latest versions only
            List<TradeQueryResponse> latestTrades = trades.getContent().stream()
                    .collect(Collectors.toMap(
                            Trade::getTradeId,
                            t -> t,
                            (t1, t2) -> t1.getVersion() > t2.getVersion() ? t1 : t2
                    ))
                    .values()
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return new org.springframework.data.domain.PageImpl<>(
                    latestTrades, pageable, latestTrades.size());
        }
        
        return trades.map(this::convertToResponse);
    }
    
    public List<TradeQueryResponse> getTradeById(String tradeId, Boolean latestOnly) {
        if (Boolean.TRUE.equals(latestOnly)) {
            return tradeRepository.findLatestByTradeId(tradeId)
                    .map(t -> List.of(convertToResponse(t)))
                    .orElse(List.of());
        } else {
            return tradeRepository.findByTradeId(tradeId).stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }
    }
    
    private TradeQueryResponse convertToResponse(Trade trade) {
        return TradeQueryResponse.builder()
                .id(trade.getId())
                .tradeId(trade.getTradeId())
                .version(trade.getVersion())
                .counterPartyId(trade.getCounterPartyId())
                .bookId(trade.getBookId())
                .maturityDate(trade.getMaturityDate())
                .createdDate(trade.getCreatedDate())
                .expired(trade.getExpired())
                .status(trade.getStatus())
                .lastUpdatedAt(trade.getLastUpdatedAt())
                .build();
    }
    
    private Page<TradeQueryResponse> convertToPage(List<Trade> trades, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), trades.size());
        
        List<TradeQueryResponse> content = trades.subList(start, end).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(
                content, pageable, trades.size());
    }
}

