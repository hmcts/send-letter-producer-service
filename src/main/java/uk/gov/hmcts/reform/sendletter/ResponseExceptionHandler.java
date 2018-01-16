package uk.gov.hmcts.reform.sendletter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sendletter.exceptions.LetterAlreadySentException;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity handleInvalidTokenException() {
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(LetterAlreadySentException.class)
    protected ResponseEntity handleLetterAlreadySentException() {
        return badRequest().body("Can't send the same letter twice");
    }
}
