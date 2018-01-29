package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class SendLetterControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private LetterService letterService;
    @MockBean
    private AuthTokenValidator tokenValidator;

    private static final String LETTER_JSON = "{"
        + "\"template\": \"abc\","
        + "\"values\": { \"a\": \"b\" },"
        + "\"type\": \"typeA\""
        + "}";

    @Test
    public void should_return_message_id_when_letter_is_successfully_sent() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        given(letterService.send(Mockito.any(Letter.class))).willReturn("12345");

        sendLetter(LETTER_JSON)
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("12345")));

        verify(tokenValidator).getServiceName("auth-header-value");
        verify(letterService).send(Mockito.any(Letter.class));
        verifyNoMoreInteractions(tokenValidator, letterService);
    }

    @Test
    public void should_return_service_bus_exception_when_service_fails_due_to_service_bus() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        given(letterService.send(Mockito.any(Letter.class))).willThrow(ServiceBusException.class);

        sendLetter(LETTER_JSON)
            .andExpect(status().is5xxServerError())
            .andExpect(content().string(
                containsString("Exception occured while communicating with service bus")));

        verify(tokenValidator).getServiceName("auth-header-value");
        verifyNoMoreInteractions(tokenValidator);
    }

    @Test
    public void should_return_interrupted_exception_when_service_fails_due_to_thread_interruption() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        given(letterService.send(Mockito.any(Letter.class))).willThrow(InterruptedException.class);

        sendLetter(LETTER_JSON)
            .andExpect(status().is5xxServerError())
            .andExpect(content().string(
                containsString("Exception occurred as the thread was interrupted")));

        verify(tokenValidator).getServiceName("auth-header-value");
        verifyNoMoreInteractions(tokenValidator);
    }

    @Test
    public void should_return_json_processing_exception_when_service_fails_to_serialize_letter() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        given(letterService.send(Mockito.any(Letter.class))).willThrow(JsonProcessingException.class);

        sendLetter(LETTER_JSON)
            .andExpect(status().is4xxClientError())
            .andExpect(content().string(
                containsString("Exception occured while parsing letter contents")));


        verify(tokenValidator).getServiceName("auth-header-value");
        verifyNoMoreInteractions(tokenValidator);

    }


    @Test
    public void should_return_400_client_error_when_invalid_letter_is_sent() throws Exception {
        sendLetter("").andExpect(status().is4xxClientError());

        verify(letterService, never()).send(any(Letter.class));
    }

    private ResultActions sendLetter(String json) throws Exception {
        return mockMvc.perform(
            post("/letters")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "auth-header-value")
                .content(json)
        );
    }
}
