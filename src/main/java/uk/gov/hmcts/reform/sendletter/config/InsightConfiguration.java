package uk.gov.hmcts.reform.sendletter.config;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import com.microsoft.applicationinsights.web.spring.internal.InterceptorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@Configuration
@Import(InterceptorRegistry.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class InsightConfiguration {

    @Bean
    public TelemetryClient getTelemetryClient() {
        return new TelemetryClient();
    }

    @Bean
    @ConditionalOnWebApplication
    public FilterRegistrationBean registerWebRequestTrackingFilter(
        @Value("${spring.application.name}") String appName
    ) {
        FilterRegistrationBean bean = new FilterRegistrationBean();

        bean.setFilter(new WebRequestTrackingFilter(appName));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return bean;
    }
}
