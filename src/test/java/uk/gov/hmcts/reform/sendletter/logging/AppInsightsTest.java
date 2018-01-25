package uk.gov.hmcts.reform.sendletter.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppInsightsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private TelemetryClient telemetry;

    private final TelemetryContext context = new TelemetryContext();

    @Before
    public void setUp() {
        when(telemetry.getContext()).thenReturn(context);
    }

    @After
    public void tearDown() {
        reset(telemetry);
    }

    @Test
    public void should_fail_creating_app_insights_when_instrumentation_key_is_not_present() {
        context.setInstrumentationKey(null);

        exception.expect(NullPointerException.class);
        exception.expectMessage("Missing APPLICATION_INSIGHTS_IKEY environment variable");

        new AppInsights(telemetry, true);
    }

    @Test
    public void should_create_app_insights_with_dev_mode_on() {
        context.setInstrumentationKey("some-key");

        new AppInsights(telemetry, true);

        assertThat(TelemetryConfiguration.getActive().getChannel().isDeveloperMode()).isTrue();
    }

    @Test
    public void should_create_app_insights_with_dev_mode_off() {
        context.setInstrumentationKey("some-key");

        new AppInsights(telemetry, false);

        assertThat(TelemetryConfiguration.getActive().getChannel().isDeveloperMode()).isFalse();
    }
}
