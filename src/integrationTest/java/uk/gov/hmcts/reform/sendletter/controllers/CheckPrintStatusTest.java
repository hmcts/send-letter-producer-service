package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.out.NotPrintedLetter;
import uk.gov.hmcts.reform.sendletter.util.MessageIdProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
public class CheckPrintStatusTest {

    private static final String serviceName = "send_letter_consumer";

    private static final LocalTime fivePm = LocalTime.of(17, 0, 0);

    private static final LocalTime secondBeforeFivePm = LocalTime.of(16, 59, 59);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @SpyBean
    private AppInsights insights;

    @SpyBean
    private LetterRepository letterRepository;

    @Before
    public void setUp() {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn(serviceName);
    }

    @Test
    public void should_return_204_when_there_are_no_unprinted_letters() throws Exception {
        ArgumentCaptor<NotPrintedLetter> captor = ArgumentCaptor.forClass(NotPrintedLetter.class);

        checkPrintStatus().andExpect(status().isNoContent());

        verify(letterRepository).getStaleLetters();
        verify(insights, never()).trackNotPrintedLetter(captor.capture());

        assertThat(captor.getAllValues()).isEmpty();
    }

    @Test
    public void should_return_204_when_there_is_an_unprinted_letter() throws Exception {
        UUID letterId = createLetterAndGetId();
        setSentToPrintAt(letterId, secondBeforeFivePm);

        ArgumentCaptor<NotPrintedLetter> captor = ArgumentCaptor.forClass(NotPrintedLetter.class);

        checkPrintStatus().andExpect(status().isNoContent());

        verify(insights).trackNotPrintedLetter(captor.capture());

        assertThat(captor.getAllValues()).hasSize(1);
        assertThat(captor.getValue().id).isEqualByComparingTo(letterId);
        assertThat(captor.getValue().sentToPrintAt)
            .isEqualByComparingTo(ZonedDateTime.now(ZoneOffset.UTC).with(secondBeforeFivePm).minusDays(1));
    }

    @Test
    public void should_not_pick_up_letter_if_sent_to_print_happened_after_the_deadline() throws Exception {
        setSentToPrintAt(createLetterAndGetId(), fivePm);
        checkPrintStatus();

        ArgumentCaptor<NotPrintedLetter> captor = ArgumentCaptor.forClass(NotPrintedLetter.class);

        verify(insights, never()).trackNotPrintedLetter(captor.capture());

        assertThat(captor.getAllValues()).isEmpty();
    }

    @Test
    public void should_not_pick_up_letter_if_not_sent_to_print() throws Exception {
        createLetterAndGetId();
        checkPrintStatus();

        ArgumentCaptor<NotPrintedLetter> captor = ArgumentCaptor.forClass(NotPrintedLetter.class);

        verify(insights, never()).trackNotPrintedLetter(captor.capture());

        assertThat(captor.getAllValues()).isEmpty();
    }

    @Test
    public void should_not_pick_up_letter_if_it_is_marked_as_failed() throws Exception {
        UUID letterId = createLetterAndGetId();
        setSentToPrintAt(letterId, secondBeforeFivePm);
        letterRepository.updateIsFailed(letterId);
        checkPrintStatus();

        ArgumentCaptor<NotPrintedLetter> captor = ArgumentCaptor.forClass(NotPrintedLetter.class);

        verify(insights, never()).trackNotPrintedLetter(captor.capture());

        assertThat(captor.getAllValues()).isEmpty();
    }

    @Test
    public void should_not_pick_up_letter_if_it_is_already_printed() throws Exception {
        UUID letterId = createLetterAndGetId();
        setSentToPrintAt(letterId, secondBeforeFivePm);
        letterRepository.updatePrintedAt(letterId, LocalDateTime.now());
        checkPrintStatus();

        ArgumentCaptor<NotPrintedLetter> captor = ArgumentCaptor.forClass(NotPrintedLetter.class);

        verify(insights, never()).trackNotPrintedLetter(captor.capture());

        assertThat(captor.getAllValues()).isEmpty();
    }

    private ResultActions checkPrintStatus() throws Exception {
        return mvc.perform(post("/letter-reports/print-status-check")
            .header("ServiceAuthorization", "auth-header-value")
        );
    }

    private UUID createLetterAndGetId() throws JsonProcessingException {
        UUID letterId = UUID.randomUUID();
        Letter letter = new Letter(Collections.emptyList(), "some-type", Collections.emptyMap());
        DbLetter dbLetter = new DbLetter(letterId, serviceName, letter);
        Instant created = Instant.now().minus(2, ChronoUnit.DAYS);

        letterRepository.save(dbLetter, created, MessageIdProvider.randomMessageId());

        return letterId;
    }

    private void setSentToPrintAt(UUID letterId, LocalTime time) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        letterRepository.updateSentToPrintAt(letterId, yesterday.with(time));
    }
}
