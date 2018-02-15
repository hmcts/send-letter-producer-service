package uk.gov.hmcts.reform.sendletter.exception;

public class SendMessageException extends RuntimeException {
    public SendMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
