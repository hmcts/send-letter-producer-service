package uk.gov.hmcts.reform.sendletter.config;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

@Configuration
public class MockConfiguration {

    @Autowired
    private ApplicationContext context;

    @Bean
    @Primary
    public SentLettersCache sentLettersCache(@Value("${redis.enabled}") boolean isRedisEnabled) {
        return (SentLettersCache) context.getBean(isRedisEnabled ? "redisCache" : "inMemoryCache");
    }

    @Bean
    @Primary
    public LetterChecksumGenerator getGeneratorMock() {
        return Mockito.mock(LetterChecksumGenerator.class);
    }
}
