package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.domain.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
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

    @GetMapping(path = "/{id}")
    @ApiOperation(value = "Get letter status")
    @ApiResponses({
        @ApiResponse(code = 200, response = LetterStatus.class, message = "Success"),
        @ApiResponse(code = 401, message = "Invalid service authorisation header"),
        @ApiResponse(code = 404, message = "Letter not found")
    })
    public ResponseEntity<LetterStatus> getLetterStatus(
        @PathVariable String id,
        @RequestHeader("ServiceAuthorization") String serviceAuthHeader
    ) {
        UUID letterId = getLetterIdFromString(id);
        String serviceName = tokenValidator.getServiceName(serviceAuthHeader);
        LetterStatus letterStatus = letterService.getStatus(letterId, serviceName);

        return ok(letterStatus);
    }

    @PutMapping(path = "/{id}/sent-to-print-at")
    @ApiOperation(value = "Update when letter was sent to print")
    public ResponseEntity<Void> updateSentToPrint(
        @PathVariable("id") String id,
        @RequestBody LetterSentToPrintAtPatch patch,
        @RequestHeader("ServiceAuthorization") String serviceAuthHeader
    ) {
        tokenValidator.getServiceName(serviceAuthHeader); //TODO: check that this service is allowed to do it
        letterService.updateSentToPrintAt(getLetterIdFromString(id), patch);

        return noContent().build();
    }

    private UUID getLetterIdFromString(String letterId) {
        try {
            return UUID.fromString(letterId);
        } catch (IllegalArgumentException exception) {
            throw new LetterNotFoundException(letterId, exception);
        }
    }
}
