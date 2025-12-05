package com.tradeplatform.tradeservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Slf4j
@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // or "Asia/Kolkata"
        log.info("Default JVM timezone set to {}", TimeZone.getDefault());
    }
}
