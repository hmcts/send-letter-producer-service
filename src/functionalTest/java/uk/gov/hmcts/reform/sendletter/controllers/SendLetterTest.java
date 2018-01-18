package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.config.MockConfiguration;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.notify.INotifyClient;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(MockConfiguration.class)
public class SendLetterTest {

    @Autowired
    private MockMvc mvc;

    @SpyBean
    private LetterService service;

    @SpyBean
    private AuthTokenValidator validator;

    @SpyBean
    private SentLettersCache cache;

    @SpyBean
    private INotifyClient notifyClient;

    @Test
    public void should_return_200_when_single_letter_is_sent() throws Exception {
        send("{\"letter\":\"singleton\"}").andExpect(status().isOk());

        verify(cache, times(1)).add(any(Letter.class));
        verify(cache, never()).remove(any(Letter.class));
        verify(notifyClient, times(1)).send(any(Letter.class));
        String serviceName = verify(validator, times(1)).getServiceName(anyString());

        assertThat(serviceName).isNull();// make sure validator is void as per configuration
    }

    @Test
    public void should_return_400_when_sending_letter_twice() throws Exception {
        String letter = "{\"letter\":\"duplicate\"}";
        send(letter).andExpect(status().isOk());
        send(letter).andExpect(status().isBadRequest())
            .andExpect(content().string("Can't send the same letter twice"));

        verify(cache, times(2)).add(any(Letter.class));
        verify(cache, never()).remove(any(Letter.class));
        verify(notifyClient, times(1)).send(any(Letter.class));
    }

    @Test
    public void should_return_500_when_notification_has_failed() throws Exception {
        BDDMockito.willThrow(Exception.class).given(notifyClient).send(any(Letter.class));

        send("{\"letter\":\"i don't want to be notified\"}").andExpect(status().isInternalServerError());

        verify(cache, times(1)).add(any(Letter.class));
        verify(cache, times(1)).remove(any(Letter.class));
        verify(notifyClient, times(1)).send(any(Letter.class));
    }

    @Test
    public void should_return_400_when_bad_letter_is_sent() throws Exception {
        send("").andExpect(status().isBadRequest());

        verify(service, never()).send(any(Letter.class));
    }

    private ResultActions send(String content) throws Exception {
        BDDMockito
            .given(MockConfiguration.GENERATOR.generateChecksum(any(Letter.class)))
            .willReturn(content);

        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }
}
