package uk.gov.hmcts.reform.slc.services.steps.maptoletter.exceptions;

public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMessageException(String message) {
        super(message);
    }
}
