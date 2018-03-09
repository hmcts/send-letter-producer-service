package uk.gov.hmcts.reform.slc.services.steps.maptoletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.slc.logging.AppInsights;
import uk.gov.hmcts.reform.slc.model.Letter;
import uk.gov.hmcts.reform.slc.services.steps.maptoletter.exceptions.InvalidMessageException;

import java.io.IOException;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * Maps Service Bus message to a letter instance.
 */
@Component
public class LetterMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Autowired
    private AppInsights insights;

    public Letter from(IMessage msg) {
        byte[] body = msg.getBody();
        String messageId = msg.getMessageId();

        try {
            return validate(objectMapper.readValue(body, Letter.class), messageId, body.length);
        } catch (IOException exc) {
            insights.trackMessageNotMapped(messageId, body.length);

            throw new InvalidMessageException("Unable to deserialize message " + messageId, exc);
        }
    }

    private Letter validate(Letter letter, String messageId, int bodyLength) {
        if (letter == null) {
            insights.trackMessageMappedToNull(messageId);

            throw new InvalidMessageException("Empty message " + messageId);
        }

        Set<ConstraintViolation<Letter>> violations = validator.validate(letter);

        if (!violations.isEmpty()) {
            insights.trackMessageMappedToInvalid(messageId, bodyLength);

            // can work on building message from violations
            throw new InvalidMessageException("Invalid message body " + messageId);
        }

        letter.documents.forEach(document ->
            insights.trackMessageMappedToLetter(messageId, letter.service, document.template, bodyLength)
        );

        return letter;
    }
}
