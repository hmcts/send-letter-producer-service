package uk.gov.hmcts.reform.sendletter.config;

import com.google.common.base.Strings;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersInMemoryCache;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersRedisCache;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

@Configuration
public class RedisConfiguration {

    @Autowired
    private ApplicationContext context;

    @Bean("redisCache")
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
    public SentLettersCache getRedisCache(
        @Value("${redis.host}") String host,
        @Value("${redis.port}") int port,
        @Value("${redis.password}") String password,
        @Value("${ttlInSeconds}") long ttlInSeconds
    ) {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setPassword(Strings.isNullOrEmpty(password) ? null : password);

        return new SentLettersRedisCache(
            Redisson.create(config),
            context.getBean(LetterChecksumGenerator.class),
            ttlInSeconds
        );
    }

    @Bean("inMemoryCache")
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "false")
    public SentLettersCache getInMemoryCache() {
        return new SentLettersInMemoryCache(context.getBean(LetterChecksumGenerator.class));
    }
}
