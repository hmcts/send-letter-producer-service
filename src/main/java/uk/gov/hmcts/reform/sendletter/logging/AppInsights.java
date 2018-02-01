package uk.gov.hmcts.reform.sendletter.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;

@Component
public class AppInsights extends AbstractAppInsights {

    public AppInsights(TelemetryClient telemetry) {
        super(telemetry);
    }
}
