package com.tradeplatform.tradeservice.filter;

import com.tradeplatform.tradeservice.config.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Order(1) // Execute early in the filter chain
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "security.enforce-gateway-only",
        havingValue = "true",
        matchIfMissing = false)
public class IpWhitelistFilter extends OncePerRequestFilter {
    
    private final SecurityConfig securityConfig;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (!securityConfig.isEnforceGatewayOnly()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String requestPath = request.getRequestURI();
        if (securityConfig.getExcludedPaths().stream()
                .anyMatch(path -> requestPath.startsWith(path))) {
            log.debug("Skipping IP check for excluded path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = request.getRemoteAddr();
        List<String> allowedIps = securityConfig.getAllowedIps();
        
        log.info("IP Whitelist Filter - enforceGatewayOnly: {}, allowedIps: {}, clientIp: {}, path: {}", 
                securityConfig.isEnforceGatewayOnly(), allowedIps, clientIp, requestPath);
        
        if (allowedIps.contains(clientIp)) {
            log.debug("Allowed request from IP: {} to path: {}", clientIp, requestPath);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Blocked request from IP: {} to path: {} (not in whitelist: {})", 
                    clientIp, requestPath, allowedIps);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                String.format(
                    "{\"error\":\"Forbidden\",\"message\":\"Direct access not allowed. Please use the gateway.\",\"clientIp\":\"%s\",\"allowedIps\":%s}",
                    clientIp, allowedIps.toString()
                )
            );
        }
    }
    
}

