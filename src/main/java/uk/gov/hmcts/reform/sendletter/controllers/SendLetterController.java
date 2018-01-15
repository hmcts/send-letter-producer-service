package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.util.Map;

@RestController
@RequestMapping(
    path = "letters",
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
        @ApiResponse(code = 200, message = "Successfully sent letter"),
    })
    public void sendLetter(
        @RequestHeader("ServiceAuthorization") String serviceAuthHeader,
        @RequestBody Map<String, String> addressDetails
    ) {
        tokenValidator.getServiceName(serviceAuthHeader);
        // TODO: Currently all the inputs and outputs are not known.Need to update this later.
        Letter letter = new Letter();
        letterService.send(letter);
    }
}
