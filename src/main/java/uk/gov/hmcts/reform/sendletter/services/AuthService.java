package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.exception.UnauthorizedException;

import java.util.Objects;

@Component
public class AuthService {

    /** Name of the service that can update letter status. **/
    private final String statusUpdaterService;
    private final AuthTokenValidator authTokenValidator;

    public AuthService(
        @Value("${status-update-service-name}") String statusUpdaterService,
        AuthTokenValidator authTokenValidator
    ) {
        this.statusUpdaterService = statusUpdaterService;
        this.authTokenValidator = authTokenValidator;
    }

    public void assertCanUpdateLetter(String serviceName) {
        if (!Objects.equals(serviceName, statusUpdaterService)) {
            throw new UnauthorizedException("Service " + serviceName + " does not have permissions to update letters");
        }
    }

    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }
}
