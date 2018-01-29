package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import javax.validation.Valid;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequestMapping(
    path = "/letters",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class SendLetterController {

    private final LetterService letterService;
    private final AuthTokenValidator tokenValidator;

    public SendLetterController(
        LetterService letterService,
        AuthTokenValidator tokenValidator
    ) {
        this.letterService = letterService;
        this.tokenValidator = tokenValidator;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ApiOperation(value = "Send letter to print and post service")
    @ApiResponses({
        @ApiResponse(code = 200, response = String.class, message = "Successfully sent letter")
    })
    public ResponseEntity<String> sendLetter(
        @RequestHeader("ServiceAuthorization") String serviceAuthHeader,
        @Valid @RequestBody Letter letter
    ) throws ServiceBusException, InterruptedException, JsonProcessingException {

        tokenValidator.getServiceName(serviceAuthHeader);

        String messageId = letterService.send(letter);

        return ok().body(messageId);
    }
}
