package com.tradeplatform.tradegateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitLoggingFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        if (!"POST".equals(method) || path == null || !path.contains("/api/trades") || path.contains("/cache")) {
            return chain.filter(exchange);
        }
        
        // Read request body to extract tradeId and version
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                
                String body = new String(bytes, StandardCharsets.UTF_8);
                final String[] tradeInfo = extractTradeInfo(body);
                
                log.info("Received trade submission: tradeId={}, version={}", tradeInfo[0], tradeInfo[1]);
                
                ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        if (bytes.length > 0) {
                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                            return Flux.just(buffer);
                        }
                        return Flux.empty();
                    }
                };
                
                ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
                    @Override
                    public boolean setStatusCode(org.springframework.http.HttpStatusCode status) {
                        boolean result = super.setStatusCode(status);
                        if (status != null && status.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                            log.warn("Rate limit exceeded for tradeId={}, version={}", tradeInfo[0], tradeInfo[1]);
                        }
                        return result;
                    }
                };
                
                ServerWebExchange decoratedExchange = exchange.mutate()
                    .request(requestDecorator)
                    .response(responseDecorator)
                    .build();
                
                return chain.filter(decoratedExchange);
            })
            .onErrorResume(e -> {
                log.warn("Failed to read request body for logging: {}", e.getMessage());
                return chain.filter(exchange);
            });
    }
    
    private String[] extractTradeInfo(String body) {
        String tradeId = "unknown";
        String version = "unknown";
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.has("tradeId")) {
                tradeId = json.get("tradeId").asText();
            }
            if (json.has("version")) {
                version = json.get("version").asText();
            }
        } catch (Exception e) {
            // ignore
        }
        return new String[]{tradeId, version};
    }

    @Override
    public int getOrder() {
        // Run early to wrap the response before rate limiter processes it
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

