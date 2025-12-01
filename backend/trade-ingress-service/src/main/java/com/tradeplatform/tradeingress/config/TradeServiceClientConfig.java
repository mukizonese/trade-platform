package com.tradeplatform.tradeingress.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple WebClient configuration for calling trade-service.
 * Uses Spring's default ObjectMapper with a custom Page deserializer.
 */
@Configuration
public class TradeServiceClientConfig {
    
    @Value("${trade.service.url:http://localhost:8080}")
    private String tradeServiceUrl;
    
    @Bean
    public WebClient tradeServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        
        // Create ObjectMapper with Java 8 time support and Page deserializer
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Required for LocalDate, LocalDateTime, etc.
        
        // Add Page deserializer
        SimpleModule pageModule = new SimpleModule("PageModule");
        pageModule.addDeserializer(Page.class, new PageDeserializer(mapper));
        mapper.registerModule(pageModule);
        
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper));
                })
                .build();
        
        return WebClient.builder()
                .baseUrl(tradeServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
    
    /**
     * Simple deserializer for Spring Data Page interface.
     * Converts JSON Page response to PageImpl.
     */
    private static class PageDeserializer extends JsonDeserializer<Page<?>> implements ContextualDeserializer {
        private final ObjectMapper mapper;
        private final JavaType contentType;
        
        PageDeserializer(ObjectMapper mapper) {
            this(mapper, null);
        }
        
        PageDeserializer(ObjectMapper mapper, JavaType contentType) {
            this.mapper = mapper;
            this.contentType = contentType;
        }
        
        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            JavaType type = ctxt.getContextualType();
            JavaType contentType = (type != null && type.hasGenericTypes()) ? type.containedType(0) : null;
            return new PageDeserializer(mapper, contentType);
        }
        
        @Override
        public Page<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            // Deserialize content array
            List<Object> content = new ArrayList<>();
            JsonNode contentNode = node.get("content");
            if (contentNode != null && contentNode.isArray()) {
                for (JsonNode item : contentNode) {
                    Object itemObj = (contentType != null) 
                        ? mapper.treeToValue(item, contentType)
                        : mapper.treeToValue(item, Object.class);
                    content.add(itemObj);
                }
            }
            
            // Extract pagination info
            int page = 0;
            int size = 20;
            JsonNode pageable = node.get("pageable");
            if (pageable != null) {
                JsonNode pageNumber = pageable.get("pageNumber");
                JsonNode pageSize = pageable.get("pageSize");
                if (pageNumber != null) page = pageNumber.asInt();
                if (pageSize != null) size = pageSize.asInt();
            }
            
            long totalElements = node.has("totalElements") 
                ? node.get("totalElements").asLong() 
                : content.size();
            
            return new PageImpl<>(content, PageRequest.of(page, size), totalElements);
        }
    }
}

