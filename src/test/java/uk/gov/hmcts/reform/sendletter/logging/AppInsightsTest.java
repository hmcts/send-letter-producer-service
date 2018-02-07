package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppInsightsTest {

    private static final String IKEY = "some-key";
    private static final String MESSAGE_ID = "some-message-id";
    private static final String SERVICE_NAME = "some-service-name";
    private static final String TEMPLATE = "some-template";

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

        new AppInsights(telemetry);
    }

    @Test
    public void should_create_app_insights_with_default_dev_mode_off() {
        context.setInstrumentationKey(IKEY);

        new AppInsights(telemetry);

        assertThat(TelemetryConfiguration.getActive().getChannel().isDeveloperMode()).isFalse();
    }

    @Test
    public void should_track_message_acknowledgement_event_for_success_case() {
        context.setInstrumentationKey(IKEY);

        AppInsights insights = new AppInsights(telemetry);

        insights.trackMessageAcknowledgement(java.time.Duration.ofMinutes(1), true, MESSAGE_ID);

        verify(telemetry).getContext();
        verify(telemetry).trackDependency(
            eq(AppInsights.SERVICE_BUS_DEPENDENCY),
            eq(AppInsights.SERVICE_BUS_MESSAGE_ACKNOWLEDGED),
            any(Duration.class),
            eq(true)
        );
        verify(telemetry).trackEvent(
            eq(AppInsights.SERVICE_BUS_MESSAGE_ACKNOWLEDGED),
            eq(Collections.singletonMap("messageId", MESSAGE_ID)),
            eq(null)
        );
        verifyNoMoreInteractions(telemetry);
    }

    @Test
    public void should_track_message_acknowledgement_event_for_fail_case() {
        context.setInstrumentationKey(IKEY);

        AppInsights insights = new AppInsights(telemetry);

        insights.trackMessageAcknowledgement(java.time.Duration.ofMinutes(1), false, MESSAGE_ID);

        verify(telemetry).getContext();
        verify(telemetry).trackDependency(
            eq(AppInsights.SERVICE_BUS_DEPENDENCY),
            eq(AppInsights.SERVICE_BUS_MESSAGE_ACKNOWLEDGED),
            any(Duration.class),
            eq(false)
        );
        verifyNoMoreInteractions(telemetry);
    }

    @Test
    public void should_track_exception() {
        context.setInstrumentationKey(IKEY);

        AppInsights insights = new AppInsights(telemetry);

        insights.trackException(new NullPointerException("Some null"));

        verify(telemetry).trackException(any(NullPointerException.class));
    }

    @Test
    public void should_track_message_received_event() {
        context.setInstrumentationKey(IKEY);

        AppInsights insights = new AppInsights(telemetry);

        insights.trackMessageReceived(SERVICE_NAME, TEMPLATE, MESSAGE_ID);

        Map<String, String> properties = ImmutableMap.of(
            "service", SERVICE_NAME,
            "template", TEMPLATE,
            "messageId", MESSAGE_ID
        );

        verify(telemetry).trackEvent(
            eq(AppInsights.MESSAGE_RECEIVED),
            eq(properties),
            eq(null)
        );
    }
}
