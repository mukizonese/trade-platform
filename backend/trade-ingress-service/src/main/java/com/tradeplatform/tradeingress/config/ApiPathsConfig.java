package com.tradeplatform.tradeingress.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api.paths")
@Data
public class ApiPathsConfig {
    
    /**
     * Base path for trades API (default: /api/trades)
     */
    private String trades = "/api/trades";
    
    /**
     * Base path for cache API (default: /api/trades/cache)
     */
    private String cache = "/api/trades/cache";
    
    /**
     * Base path for audit API (default: /api/audit)
     */
    private String audit = "/api/audit";
}

