package uk.gov.hmcts.reform.sendletter.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InsightConfiguration {

    @Bean
    public TelemetryClient getTelemetryClient() {
        return new TelemetryClient();
    }
}
