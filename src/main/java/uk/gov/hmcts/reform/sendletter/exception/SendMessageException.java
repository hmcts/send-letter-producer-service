package uk.gov.hmcts.reform.sendletter.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SendMessageException extends UnknownErrorCodeException {
    public SendMessageException(String message, Throwable cause) {
        super(AlertLevel.P1, message, cause);
    }
}
