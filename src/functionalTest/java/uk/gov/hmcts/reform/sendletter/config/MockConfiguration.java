package uk.gov.hmcts.reform.sendletter.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersInMemoryCache;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

@TestConfiguration
public class MockConfiguration {

    public static final LetterChecksumGenerator GENERATOR = Mockito.mock(LetterChecksumGenerator.class);

    @Bean
    @Primary
    public SentLettersCache withGeneratorMock() {
        return new SentLettersInMemoryCache(GENERATOR);
    }
}
