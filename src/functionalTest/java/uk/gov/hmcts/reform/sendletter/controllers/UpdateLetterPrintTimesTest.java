package uk.gov.hmcts.reform.sendletter.controllers;

import com.microsoft.azure.servicebus.IQueueClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.domain.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest
public class UpdateLetterPrintTimesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterRepository letterRepository;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @MockBean
    private QueueClientSupplier queueClientSupplier;

    @Mock
    private IQueueClient queueClient;

    @Value("${status-update-service-name}")
    private String serviceCanMakeUpdate;

    @Test
    public void should_return_204_after_updating_letter_in_db_and_verify_the_changes() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn(serviceCanMakeUpdate);
        given(queueClientSupplier.get()).willReturn(queueClient);

        UUID letterId = UUID.randomUUID();
        Letter letter = new Letter(Collections.emptyList(), "some-type", Collections.emptyMap());
        DbLetter dbLetter = new DbLetter(letterId, serviceCanMakeUpdate, letter);
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);

        letterRepository.save(dbLetter, createdAt.toInstant(), "some-message-id");

        ZonedDateTime updated = ZonedDateTime.now(ZoneOffset.UTC);

        updateLetterStatus(letterId, "sent_to_print_at", updated).andExpect(status().isNoContent());
        updateLetterStatus(letterId, "printed_at", updated.plusHours(1)).andExpect(status().isNoContent());

        Optional<LetterStatus> result = letterRepository.getLetterStatus(letterId, serviceCanMakeUpdate);

        assertThat(result.isPresent()).isTrue();

        LetterStatus actualStatus = result.orElse(null);
        LetterStatus expectedStatus = new LetterStatus(
            letterId, "some-message-id", createdAt, updated, updated.plusHours(1), false
        );

        assertThat(actualStatus).isEqualToComparingFieldByField(expectedStatus);
    }

    @Test
    public void should_return_403_when_service_is_not_authorised_to_update_letter() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-cannot-make-update");

        updateLetterStatus(UUID.randomUUID(), "sent_to_print_at")
            .andExpect(status().isForbidden());
        updateLetterStatus(UUID.randomUUID(), "printed_at")
            .andExpect(status().isForbidden());
    }

    @Test
    public void should_return_404_when_letter_is_not_found() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn(serviceCanMakeUpdate);

        updateLetterStatus(UUID.randomUUID(), "sent_to_print_at")
            .andExpect(status().isNotFound());
        updateLetterStatus(UUID.randomUUID(), "printed_at")
            .andExpect(status().isNotFound());
    }

    private ResultActions updateLetterStatus(UUID letterId, String field, ZonedDateTime time) throws Exception {
        MockHttpServletRequestBuilder request =
            put("/letters/" + letterId.toString() + "/" + field.replaceAll("_", "-"))
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"" + field + "\":\"" + time.toString() + "\"}");

        return mvc.perform(request);
    }

    private ResultActions updateLetterStatus(UUID letterId, String field) throws Exception {
        return updateLetterStatus(letterId, field, ZonedDateTime.now(ZoneOffset.UTC));
    }
}
