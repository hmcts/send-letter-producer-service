package uk.gov.hmcts.reform.sendletter.logging;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;
import uk.gov.hmcts.reform.sendletter.model.out.NotPrintedLetter;

import java.time.ZonedDateTime;
import java.util.Collections;

import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public class AppInsights extends AbstractAppInsights {

    private static final int SECONDS_PER_DAY = 86_400;

    static final String SERVICE_BUS_DEPENDENCY = "ServiceBus";
    static final String SERVICE_BUS_MESSAGE_ACKNOWLEDGED = "MessageAcknowledged";

    static final String LETTER_NOT_PRINTED = "LetterNotPrinted";

    public AppInsights(TelemetryClient telemetry) {
        super(telemetry);
    }

    public void trackMessageAcknowledgement(java.time.Duration duration, boolean success, String messageId) {
        telemetry.trackDependency(
            SERVICE_BUS_DEPENDENCY,
            SERVICE_BUS_MESSAGE_ACKNOWLEDGED,
            new Duration(duration.toMillis()),
            success
        );

        if (success) {
            telemetry.trackEvent(
                SERVICE_BUS_MESSAGE_ACKNOWLEDGED,
                Collections.singletonMap("messageId", messageId),
                null
            );
        }
    }

    private double getSecondsSince(ZonedDateTime time) {
        long seconds = SECONDS.between(time, ZonedDateTime.now());

        return ((double) seconds) / SECONDS_PER_DAY;
    }

    public void trackNotPrintedLetter(NotPrintedLetter notPrintedLetter) {
        telemetry.trackEvent(
            LETTER_NOT_PRINTED,
            ImmutableMap.of(
                "letterId", notPrintedLetter.id.toString(),
                "messageId", notPrintedLetter.messageId,
                "service", notPrintedLetter.service,
                "type", notPrintedLetter.type
            ),
            ImmutableMap.of(
                "daysCreated", getSecondsSince(notPrintedLetter.createdAt),
                "daysSentToPrint", getSecondsSince(notPrintedLetter.sentToPrintAt)
            )
        );
    }

    public void trackException(Exception exception) {
        telemetry.trackException(exception);
    }
}
