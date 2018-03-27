package uk.gov.hmcts.reform.sendletter.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

import java.util.UUID;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class LetterNotFoundException extends UnknownErrorCodeException {

    public LetterNotFoundException(String letterId, Throwable cause) {
        super(AlertLevel.P4, "Letter with ID '" + letterId + "' not found", cause);
    }

    public LetterNotFoundException(UUID id) {
        this(id.toString(), null);
    }
}
