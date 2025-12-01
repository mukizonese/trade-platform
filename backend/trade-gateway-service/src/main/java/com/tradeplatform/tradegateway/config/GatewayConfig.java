package com.tradeplatform.tradegateway.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple gateway configuration for Netty server settings.
 */
@Configuration
public class GatewayConfig {
    
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> factory.addServerCustomizers(
            (NettyServerCustomizer) httpServer -> httpServer.httpRequestDecoder(
                spec -> spec.maxHeaderSize(131072) // 128KB for forwarded headers
            )
        );
    }
}

