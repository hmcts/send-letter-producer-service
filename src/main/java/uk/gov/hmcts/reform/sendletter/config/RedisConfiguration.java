package uk.gov.hmcts.reform.sendletter.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersNullCache;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersRedisCache;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

@Configuration
public class RedisConfiguration {

    @Bean
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
    public SentLettersCache getRedisCache(
        @Value("${redis.host}") String host,
        @Value("${redis.port}") int port,
        @Value("${redis.password}") String password,
        @Value("${ttlInSeconds}") long ttlInSeconds
    ) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress(host + ":" + port)
            .setPassword(password);

        return new SentLettersRedisCache(
            Redisson.create(config),
            new LetterChecksumGenerator(),
            ttlInSeconds
        );
    }

    @Bean
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "false")
    public SentLettersCache getNullCache() {
        return new SentLettersNullCache();
    }
}
