package com.tradeplatform.tradeingress.client;

import com.tradeplatform.common.dto.TradeDto;
import com.tradeplatform.common.dto.TradeQueryResponse;
import com.tradeplatform.common.dto.TradeSubmissionResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeServiceClient {
    
    private final WebClient tradeServiceWebClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public Mono<TradeSubmissionResponse> submitTrade(TradeDto tradeDto, String source) {
        return tradeServiceWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/trades")
                        .queryParam("source", source)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tradeDto)
                .retrieve()
                .bodyToMono(TradeSubmissionResponse.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("tradeService")));
    }
    
    public Mono<Page<TradeQueryResponse>> getTrades(Boolean latestOnly, String tradeId, 
                                                     String bookId, String status, 
                                                     int page, int size) {
        return tradeServiceWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/trades");
                    if (latestOnly != null) {
                        uriBuilder.queryParam("latestOnly", latestOnly);
                    }
                    if (tradeId != null) {
                        uriBuilder.queryParam("tradeId", tradeId);
                    }
                    if (bookId != null) {
                        uriBuilder.queryParam("bookId", bookId);
                    }
                    if (status != null) {
                        uriBuilder.queryParam("status", status);
                    }
                    uriBuilder.queryParam("page", page);
                    uriBuilder.queryParam("size", size);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Page<TradeQueryResponse>>() {});
    }
    
    public Mono<List<TradeQueryResponse>> getTradeById(String tradeId, Boolean latestOnly) {
        return tradeServiceWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/trades/{tradeId}");
                    if (latestOnly != null) {
                        uriBuilder.queryParam("latestOnly", latestOnly);
                    }
                    return uriBuilder.build(tradeId);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TradeQueryResponse>>() {})
                .doOnError(error -> log.error("Error calling trade-service getTradeById", error));
    }
    
    public Mono<List<Map<String, Object>>> getAllTradesFromCache() {
        return tradeServiceWebClient.get()
                .uri("/api/trades/cache")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .doOnError(error -> log.error("Error calling trade-service getAllTradesFromCache", error));
    }
    
    public Mono<List<Map<String, Object>>> getTradesFromCacheByTradeId(String tradeId) {
        return tradeServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/trades/cache/{tradeId}").build(tradeId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .doOnError(error -> log.error("Error calling trade-service getTradesFromCacheByTradeId", error));
    }
    
    public Mono<Page<com.tradeplatform.common.dto.AuditDto>> getAuditLogs(int page, int size) {
        return tradeServiceWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/audit/trades");
                    uriBuilder.queryParam("page", page);
                    uriBuilder.queryParam("size", size);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Page<com.tradeplatform.common.dto.AuditDto>>() {})
                .doOnError(error -> log.error("Error calling trade-service getAuditLogs", error));
    }
}

