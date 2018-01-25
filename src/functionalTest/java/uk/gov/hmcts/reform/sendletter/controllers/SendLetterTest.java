package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.FunSuite;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.notify.INotifyClient;
import uk.gov.hmcts.reform.sendletter.notify.NotifyClientStub;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest
public class SendLetterTest extends FunSuite {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LetterChecksumGenerator generator;

    @SpyBean
    private LetterService service;

    @SpyBean
    private AuthTokenValidator validator;

    @SpyBean
    private INotifyClient notifyClient;

    private static final String LETTER_JSON = "{"
        + "\"template\": \"abc\","
        + "\"values\": { \"a\": \"b\" },"
        + "\"type\": \"typeA\""
        + "}";

    @Test
    public void check_integration_spies() {
        // just making sure using correct implementation instead of mocks/stubs
        assertThat(notifyClient.getClass().getSimpleName()).startsWith(NotifyClientStub.class.getSimpleName());
    }

    @Test
    public void should_return_200_when_single_letter_is_sent() throws Exception {
        send(LETTER_JSON).andExpect(status().isOk());

        verify(notifyClient, times(1)).send(any(Letter.class));
        String serviceName = verify(validator, times(1)).getServiceName(anyString());

        assertThat(serviceName).isNull();// make sure validator is void as per configuration
    }

    @Test
    public void should_return_500_when_notification_has_failed() throws Exception {
        BDDMockito.willThrow(Exception.class).given(notifyClient).send(any(Letter.class));

        send(LETTER_JSON).andExpect(status().isInternalServerError());

        verify(notifyClient, times(1)).send(any(Letter.class));
    }

    @Test
    public void should_return_400_when_bad_letter_is_sent() throws Exception {
        send("").andExpect(status().isBadRequest());

        verify(service, never()).send(any(Letter.class));
    }

    private ResultActions send(String content) throws Exception {
        BDDMockito
            .given(generator.generateChecksum(any(Letter.class)))
            .willReturn(content);

        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }
}
