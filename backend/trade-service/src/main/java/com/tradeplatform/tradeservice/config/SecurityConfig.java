package com.tradeplatform.tradeservice.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityConfig {
    
    private boolean enforceGatewayOnly = false;
    
    private String allowedIpsString = "127.0.0.1,::1,0:0:0:0:0:0:0:1,localhost";
    
    private List<String> allowedIps = new ArrayList<>();
    
    private String excludedPathsString = "/actuator/health,/actuator/info,/actuator/metrics";
    
    private List<String> excludedPaths = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        String defaultAllowedIps = "127.0.0.1,::1,0:0:0:0:0:0:0:1,localhost";
        
        if (StringUtils.hasText(allowedIpsString)) {
            allowedIps = Arrays.stream(allowedIpsString.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        } else if (allowedIpsString != null && allowedIpsString.isEmpty()) {
            allowedIps = List.of();
        } else {
            allowedIps = Arrays.stream(defaultAllowedIps.split(","))
                    .map(String::trim)
                    .toList();
        }
        
        if (StringUtils.hasText(excludedPathsString)) {
            excludedPaths = Arrays.stream(excludedPathsString.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        } else {
            excludedPaths = List.of("/actuator/health", "/actuator/info", "/actuator/metrics");
        }
    }
}

