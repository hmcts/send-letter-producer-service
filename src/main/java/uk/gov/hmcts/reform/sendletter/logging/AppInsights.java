package uk.gov.hmcts.reform.sendletter.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
public class AppInsights {

    private final TelemetryClient telemetry;

    public AppInsights(TelemetryClient telemetry, @Value("${app-insights.dev-mode}") boolean devMode) {
        requireNonNull(
            telemetry.getContext().getInstrumentationKey(),
            "Missing APPLICATION_INSIGHTS_IKEY environment variable"
        );

        this.telemetry = telemetry;

        TelemetryConfiguration.getActive().getChannel().setDeveloperMode(devMode);
    }
}
