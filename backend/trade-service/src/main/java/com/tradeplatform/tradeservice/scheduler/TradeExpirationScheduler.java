package com.tradeplatform.tradeservice.scheduler;

import com.tradeplatform.tradeservice.service.TradeCommandService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeExpirationScheduler {
    
    private final TradeCommandService tradeCommandService;
    
    @Value("${trade.scheduler.expiration.cron:0 0 * * * ?}")
    private String cronExpression;
    
    @PostConstruct
    public void init() {
        String scheduleDescription = getCronDescription(cronExpression);
        String envValue = System.getenv("TRADE_EXPIRATION_CRON");
        
        log.info("=== Trade Expiration Scheduler Initialized ===");
        log.info("Environment Variable TRADE_EXPIRATION_CRON: {}", envValue != null ? envValue : "NOT SET");
        log.info("Resolved Cron Expression: {}", cronExpression);
        log.info("Schedule: {}", scheduleDescription);
        log.info("Scheduler will check for matured trades and mark them as expired");
        
        if (envValue == null) {
            log.warn("  TRADE_EXPIRATION_CRON environment variable is not set!");
            log.warn("  Using default value: {}", cronExpression);
            log.warn("  To use custom cron, export TRADE_EXPIRATION_CRON or use ./run-local.sh script");
        } else if (!envValue.equals(cronExpression)) {
            log.warn("  Environment variable value '{}' differs from resolved value '{}'", envValue, cronExpression);
            log.warn("  Check if the variable is properly exported before starting the application");
        } else {
            log.info("✓ Environment variable successfully loaded");
        }
        log.info("================================================");
    }
    
    @Scheduled(cron = "${trade.scheduler.expiration.cron:0 0 * * * ?}")
    public void expireMaturedTrades() {
        log.info("=== Scheduled task triggered ===");
        log.debug("Cron expression: {}", cronExpression);
        
           tradeCommandService.findAndExpireMaturedTrades();
    }
    
    /**
     * Convert cron expression to human-readable description
     */
    private String getCronDescription(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return "Not configured";
        }
        
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 6) {
            return "Invalid cron format: " + cron;
        }
        
        String second = parts[0];
        String minute = parts[1];
        String hour = parts[2];
        String day = parts[3];
        String month = parts[4];
        String weekday = parts[5];
        
        // Every N minutes: "0 */N * * * ?" (e.g., "0 */5 * * * ?" for every 5 minutes)
        if ("0".equals(second) && minute.startsWith("*/") && "*".equals(hour) && 
            "*".equals(day) && "*".equals(month) && "?".equals(weekday)) {
            try {
                int interval = Integer.parseInt(minute.substring(2));
                return String.format("Every %d minute(s)", interval);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        
        // Every hour: "0 0 * * * ?"
        if ("0".equals(second) && "0".equals(minute) && "*".equals(hour) && 
            "*".equals(day) && "*".equals(month) && "?".equals(weekday)) {
            return "Every hour at the top of the hour";
        }
        
        // Every day at midnight: "0 0 0 * * ?"
        if ("0".equals(second) && "0".equals(minute) && "0".equals(hour) && 
            "*".equals(day) && "*".equals(month) && "?".equals(weekday)) {
            return "Daily at midnight";
        }
        
        // Every day at specific time: "0 0 12 * * ?"
        if ("0".equals(second) && "0".equals(minute) && !"*".equals(hour) && 
            "*".equals(day) && "*".equals(month) && "?".equals(weekday)) {
            return String.format("Daily at %s:00", hour);
        }
        
        // Default: return the cron expression
        return "Custom schedule: " + cron;
    }
}

