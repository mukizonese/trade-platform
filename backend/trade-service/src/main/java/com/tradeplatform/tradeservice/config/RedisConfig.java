package com.tradeplatform.tradeservice.config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    @Primary
    @Qualifier("redisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setPassword(password);
        
        return new LettuceConnectionFactory(config);
    }

    // @Bean
    // @Qualifier("lettuceConnectionFactory")
    // public LettuceConnectionFactory lettuceConnectionFactory() {

    //     RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration(host, port);
    //     redisConf.setPassword(password);
    //     return new LettuceConnectionFactory(redisConf);
    // }
}

