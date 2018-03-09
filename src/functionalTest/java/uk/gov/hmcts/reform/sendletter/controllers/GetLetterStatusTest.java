package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;
import uk.gov.hmcts.reform.sendletter.util.MessageIdProvider;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
public class GetLetterStatusTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @MockBean
    private QueueClientSupplier queueClientSupplier;

    @Mock
    private IQueueClient queueClient;

    @SpyBean
    private LetterRepository letterRepository;

    @Test
    public void should_return_200_after_creating_single_letter_in_db() throws Exception {
        // given
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("some-service");
        given(queueClientSupplier.get()).willReturn(queueClient);

        // and
        UUID letterId = UUID.randomUUID();
        Letter letter = new Letter(Collections.emptyList(), "some-type", Collections.emptyMap());
        DbLetter dbLetter = new DbLetter(letterId, "some-service", letter);
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);
        String messageId = MessageIdProvider.randomMessageId();

        // when
        letterRepository.save(dbLetter, createdAt.toInstant(), messageId);

        // then
        MvcResult result = getLetterStatus(letterId)
            .andExpect(status().isOk())
            .andReturn();

        String actualStatus = result.getResponse().getContentAsString();
        String expectedStatus = objectMapper.writeValueAsString(
            new LetterStatus(letterId, messageId, createdAt, null, null, false)
        );

        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    @Test
    public void should_return_404_when_letter_is_not_found() throws Exception {
        getLetterStatus(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private ResultActions getLetterStatus(UUID letterId) throws Exception {
        MockHttpServletRequestBuilder request =
            get("/letters/" + letterId.toString())
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON);

        return mvc.perform(request);
    }
}
