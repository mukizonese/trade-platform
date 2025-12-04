package com.tradeplatform.common.config;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

public class CorsConfigUtil {
    
    public static CorsConfiguration createCorsConfiguration(String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        
        // Parse allowed origins (can be comma-separated)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        origins = origins.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        
        // Use setAllowedOriginPatterns when credentials are enabled (works for both servlet and reactive)
        config.setAllowedOriginPatterns(origins);
        
        // Allow common HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow common headers
        config.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Allow credentials
        config.setAllowCredentials(true);
        
        // Expose rate limit headers
        config.setExposedHeaders(Arrays.asList(
            "X-RateLimit-Remaining",
            "X-RateLimit-Limit",
            "X-RateLimit-Requested-Tokens",
            "X-RateLimit-Burst-Capacity"
        ));
        
        // Cache preflight for 1 hour
        config.setMaxAge(3600L);
        
        return config;
    }
}

