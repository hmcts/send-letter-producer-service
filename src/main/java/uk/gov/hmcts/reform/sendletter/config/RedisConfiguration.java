package uk.gov.hmcts.reform.sendletter.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfiguration {

    @Bean
    public RedissonClient redisClient(
        @Value("${redis.host}") String host,
        @Value("${redis.port}") int port,
        @Value("${redis.password}") String password
    ) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress(host + ":" + port)
            .setPassword(password);

        return Redisson.create(config);
    }
}
