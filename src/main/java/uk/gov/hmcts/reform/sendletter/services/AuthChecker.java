package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.UnauthorizedException;

import java.util.Objects;

@Component
public class AuthChecker {

    /** Name of the service that can update letter status. **/
    private final String statusUpdaterService;

    public AuthChecker(@Value("${status-update-service-name}") String statusUpdaterService) {
        this.statusUpdaterService = statusUpdaterService;
    }

    public void assertCanUpdateLetter(String serviceName) {
        if (!Objects.equals(serviceName, statusUpdaterService)) {
            throw new UnauthorizedException("Service " + serviceName + " does not have permissions to update letters");
        }
    }
}
