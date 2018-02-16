package uk.gov.hmcts.reform.sendletter.exception;

import java.util.UUID;

public class LetterNotFoundException extends RuntimeException {

    public LetterNotFoundException(String letterId, Throwable cause) {
        super("Letter with ID '" + letterId + "' not found", cause);
    }

    public LetterNotFoundException(UUID id) {
        this(id.toString(), null);
    }
}
