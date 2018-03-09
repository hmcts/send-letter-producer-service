package uk.gov.hmcts.reform.slc.services.servicebus.exceptions;

public class PdfMergeException extends RuntimeException {
    public PdfMergeException(String message, Throwable cause) {
        super(message, cause);
    }
}
