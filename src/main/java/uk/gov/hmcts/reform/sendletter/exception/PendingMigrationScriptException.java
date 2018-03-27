package uk.gov.hmcts.reform.sendletter.exception;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class PendingMigrationScriptException extends UnknownErrorCodeException {

    public PendingMigrationScriptException(String script) {
        super(AlertLevel.P1, "Found migration not yet applied " + script);
    }
}
