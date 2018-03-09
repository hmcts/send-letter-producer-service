package uk.gov.hmcts.reform.slc.services;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.healthcheck.InternalHealth;
import uk.gov.hmcts.reform.slc.model.LetterPrintStatus;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

@Component
public class SendLetterClient {

    public static final String AUTHORIZATION_HEADER = "ServiceAuthorization";

    private static final Logger logger = LoggerFactory.getLogger(SendLetterService.class);

    private final RestTemplate restTemplate;
    private final String sendLetterProducerUrl;
    private final Supplier<ZonedDateTime> currentDateTimeSupplier;
    private final AuthTokenGenerator authTokenGenerator;

    public SendLetterClient(
        RestTemplate restTemplate,
        @Value("${sendletter.producer.url}") String sendLetterProducerUrl,
        Supplier<ZonedDateTime> currentDateTimeSupplier,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.restTemplate = restTemplate;
        this.sendLetterProducerUrl = appendIfMissing(sendLetterProducerUrl, "/");
        this.currentDateTimeSupplier = currentDateTimeSupplier;
        this.authTokenGenerator = authTokenGenerator;
    }

    public void updateSentToPrintAt(UUID letterId) {
        try {
            restTemplatePut(
                sendLetterProducerUrl + letterId + "/sent-to-print-at",
                ImmutableMap.of(
                    "sent_to_print_at",
                    currentDateTimeSupplier.get().format(ISO_INSTANT)
                )
            );
        } catch (RestClientException exception) {
            //If updating timestamp fails just log the message as the letter is already uploaded
            logger.error(
                "Exception occurred while updating sent to print time for letter id = " + letterId,
                exception
            );
        }
    }

    public void updatePrintedAt(LetterPrintStatus status) {
        restTemplatePut(
            sendLetterProducerUrl + status.id + "/printed-at",
            ImmutableMap.of(
                "printed_at",
                status.printedAt.format(ISO_INSTANT)
            )
        );
    }

    public void updateIsFailedStatus(UUID letterId) {
        try {
            restTemplatePut(sendLetterProducerUrl + letterId + "/is-failed", null);
        } catch (RestClientException exception) {
            logger.error(
                "Exception occurred while updating is failed status for letter id = " + letterId,
                exception
            );
        }
    }

    /**
     * Calls the Send Letter Producer service healthcheck.
     *
     * @return health status
     */
    public Health serviceHealthy() {
        try {
            ResponseEntity<InternalHealth> response = restTemplate
                .getForEntity(sendLetterProducerUrl + "health", InternalHealth.class);

            return Health.status(response.getBody().getStatus()).build();
        } catch (Exception ex) {
            logger.error("Error on send letter producer service healthcheck", ex);

            return Health.down(ex).build();
        }
    }

    private void restTemplatePut(String url, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        headers.add(AUTHORIZATION_HEADER, authTokenGenerator.generate());

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
    }
}
