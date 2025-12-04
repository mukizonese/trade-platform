package com.tradeplatform.tradeservice.config;

import com.tradeplatform.common.config.CorsConfigUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration for Trade Service (servlet-based).
 * Uses common CORS utility from trade-common-service.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3001}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        var corsConfig = CorsConfigUtil.createCorsConfiguration(allowedOrigins);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsFilter(source);
    }
}

