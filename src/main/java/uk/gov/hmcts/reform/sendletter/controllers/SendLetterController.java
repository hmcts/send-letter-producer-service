package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.LetterSentToPrintAtPatch;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.util.UUID;
import javax.validation.Valid;

import static org.springframework.http.ResponseEntity.noContent;
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
        @ApiResponse(code = 200, response = UUID.class, message = "Successfully sent letter")
    })
    public ResponseEntity<UUID> sendLetter(
        @RequestHeader("ServiceAuthorization") String serviceAuthHeader,
        @ApiParam(value = "Letter consisting of documents and type", required = true)
        @Valid @RequestBody Letter letter
    ) throws JsonProcessingException {

        String serviceName = tokenValidator.getServiceName(serviceAuthHeader);
        UUID letterId = letterService.send(letter, serviceName);

        return ok().body(letterId);
    }

    @PutMapping(path = "/{id}/sent-to-print-at")
    @ApiOperation(value = "Update when letter was sent to print")
    public ResponseEntity<Void> updateSentToPrint(
        @PathVariable("id") String id,
        @RequestBody LetterSentToPrintAtPatch patch,
        @RequestHeader("ServiceAuthorization") String serviceAuthHeader
    ) {
        tokenValidator.getServiceName(serviceAuthHeader); //TODO: check that this service is allowed to do it
        letterService.updateSentToPrintAt(id, patch);

        return noContent().build();
    }
}
