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
    private final String statusAccessorService;
    private final AuthTokenValidator authTokenValidator;

    public AuthService(
        @Value("${status-update-service-name}") String statusAccessorService,
        AuthTokenValidator authTokenValidator
    ) {
        this.statusAccessorService = statusAccessorService;
        this.authTokenValidator = authTokenValidator;
    }

    public void assertCanUpdateLetter(String serviceName) {
        assertAccessorCondition(serviceName, "update");
    }

    public void assertCanCheckStatus(String serviceName) {
        assertAccessorCondition(serviceName, "check");
    }

    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }

    private void assertAccessorCondition(String serviceName, String accessType) {
        if (!Objects.equals(serviceName, statusAccessorService)) {
            throw new UnauthorizedException(
                "Service " + serviceName + " does not have permissions to " + accessType + " letters state"
            );
        }
    }
}
