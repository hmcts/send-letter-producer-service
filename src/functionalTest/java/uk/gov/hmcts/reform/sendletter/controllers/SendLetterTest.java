package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
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
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
public class SendLetterTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private QueueClientSupplier queueClientSupplier;

    @SpyBean
    private AppInsights insights;

    @Mock
    private IQueueClient queueClient;

    @SpyBean
    private LetterRepository letterRepository;

    @Test
    public void should_return_200_when_single_letter_is_sent() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        MvcResult result = send(readResource("letter.json"))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotNull();

        verify(letterRepository).save(any(DbLetter.class), any(Instant.class), anyString());
        verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(true), anyString());
    }

    @Test
    public void should_return_400_when_same_letter_is_sent_twice() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        String letter = readResource("letter.json");

        send(letter).andExpect(status().isOk());
        send(letter).andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_500_when_sending_message_has_failed() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);

        willThrow(ServiceBusException.class).given(queueClient).send(any(Message.class));

        send(readResource("letter.json")).andExpect(status().isInternalServerError());

        verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(false), anyString());
    }

    @Test
    public void should_return_400_when_bad_letter_is_sent() throws Exception {
        send("").andExpect(status().isBadRequest());

        verifyNoMoreInteractions(insights);
    }

    private ResultActions send(String content) throws Exception {
        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }

    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
}
