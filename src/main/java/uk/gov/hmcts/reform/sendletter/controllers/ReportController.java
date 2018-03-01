package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@Validated
@RequestMapping(
    path = "/letter-reports",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class ReportController {

    private final LetterService letterService;
    private final AuthService authService;

    public ReportController(
        LetterService letterService,
        AuthService authService
    ) {
        this.letterService = letterService;
        this.authService = authService;
    }

    @PostMapping(path = "/print-status-check")
    @ApiOperation(value = "Execute action for print state")
    public ResponseEntity<Void> checkPrintState(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        authService.assertCanCheckStatus(serviceName);

        letterService.checkPrintState();

        return noContent().build();
    }
}
