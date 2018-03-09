package uk.gov.hmcts.reform.slc.services.steps.sftpupload.exceptions;

public class FtpStepException extends RuntimeException {
    public FtpStepException(String message, Throwable cause) {
        super(message, cause);
    }
}
