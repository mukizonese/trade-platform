package com.tradeplatform.tradeingress.controller;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {
    
    @Value("${resilience4j.ratelimiter.instances.submitTrade.limitForPeriod:2}")
    private String rateLimitLimitForPeriod;
    
    @Value("${resilience4j.ratelimiter.instances.submitTrade.limitRefreshPeriod:5s}")
    private String rateLimitRefreshPeriod;
    
    @Value("${resilience4j.ratelimiter.instances.submitTrade.timeoutDuration:0}")
    private String rateLimitTimeoutDuration;
    
    @Value("${resilience4j.circuitbreaker.instances.tradeService.slidingWindowSize:5}")
    private String circuitBreakerSlidingWindowSize;
    
    @Value("${resilience4j.circuitbreaker.instances.tradeService.minimumNumberOfCalls:10}")
    private String circuitBreakerMinimumNumberOfCalls;
    
    @Value("${resilience4j.circuitbreaker.instances.tradeService.failureRateThreshold:50}")
    private String circuitBreakerFailureRateThreshold;
    
    @Value("${resilience4j.circuitbreaker.instances.tradeService.waitDurationInOpenState:10s}")
    private String circuitBreakerWaitDurationInOpenState;
    
    @GetMapping("/actuator/config/ratelimiter")
    public ResponseEntity<RateLimiterConfig> getRateLimiterConfig() {
        RateLimiterConfig config = new RateLimiterConfig();
        config.setLimitForPeriod(rateLimitLimitForPeriod);
        config.setLimitRefreshPeriod(rateLimitRefreshPeriod);
        config.setTimeoutDuration(rateLimitTimeoutDuration);
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/actuator/config/circuitbreaker")
    public ResponseEntity<CircuitBreakerConfig> getCircuitBreakerConfig() {
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        config.setSlidingWindowSize(circuitBreakerSlidingWindowSize);
        config.setFailureRateThreshold(circuitBreakerFailureRateThreshold);
        config.setWaitDurationInOpenState(circuitBreakerWaitDurationInOpenState);
        config.setMinimumNumberOfCalls(circuitBreakerMinimumNumberOfCalls);
        return ResponseEntity.ok(config);
    }
    
    @Data
    public static class RateLimiterConfig {
        private String limitForPeriod;
        private String limitRefreshPeriod;
        private String timeoutDuration;
    }
    
    @Data
    public static class CircuitBreakerConfig {
        private String slidingWindowSize;
        private String failureRateThreshold;
        private String waitDurationInOpenState;
        private String minimumNumberOfCalls;
    }
}

