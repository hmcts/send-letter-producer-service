package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.actuate.trace.TraceProperties;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.api.filters.SensitiveHeadersRequestTraceFilter;

@Configuration
public class TracingConfiguration {

    @Bean
    public SensitiveHeadersRequestTraceFilter requestTraceFilter(
        TraceRepository traceRepository,
        TraceProperties traceProperties
    ) {
        return new SensitiveHeadersRequestTraceFilter(traceRepository, traceProperties);
    }
}
