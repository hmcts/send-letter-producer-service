package uk.gov.hmcts.reform.sendletter.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

@Configuration
public class MockConfiguration {

    @Bean
    @Primary
    public LetterChecksumGenerator getGeneratorMock() {
        return Mockito.mock(LetterChecksumGenerator.class);
    }
}
