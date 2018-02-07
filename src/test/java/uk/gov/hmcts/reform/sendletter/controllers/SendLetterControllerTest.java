package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.exception.ConnectionException;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
        given(letterService.send(any(Letter.class), anyString())).willReturn("12345");

        sendLetter(LETTER_JSON)
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("12345")));

        verify(tokenValidator).getServiceName("auth-header-value");
        verify(letterService).send(any(Letter.class), eq("service-name"));
        verifyNoMoreInteractions(tokenValidator, letterService);
    }

    @Test
    public void should_return_connection_exception_when_service_fails_due_to_service_bus() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        given(letterService.send(any(Letter.class), anyString()))
            .willThrow(
                new ConnectionException("Unable to connect to Azure service bus",
                    new ServiceBusException(false))
            );

        MvcResult mvcResult = sendLetter(LETTER_JSON)
            .andExpect(status().is5xxServerError())
            .andReturn();

        assertThat(mvcResult.getResolvedException()).isInstanceOf(ConnectionException.class);
        assertThat(mvcResult.getResolvedException().getMessage()).isEqualTo("Unable to connect to Azure service bus");
        assertThat(mvcResult.getResolvedException().getCause()).isInstanceOf(ServiceBusException.class);

        verify(tokenValidator).getServiceName("auth-header-value");
        verify(letterService).send(any(Letter.class), anyString());
        verifyNoMoreInteractions(tokenValidator, letterService);
    }

    @Test
    public void should_return_connection_exception_when_service_fails_due_to_thread_interruption() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        given(letterService.send(any(Letter.class), anyString()))
            .willThrow(
                new ConnectionException("Unable to connect to Azure service bus",
                    new InterruptedException())
            );

        MvcResult mvcResult = sendLetter(LETTER_JSON)
            .andExpect(status().is5xxServerError())
            .andReturn();

        assertThat(mvcResult.getResolvedException()).isInstanceOf(ConnectionException.class);
        assertThat(mvcResult.getResolvedException().getMessage()).isEqualTo("Unable to connect to Azure service bus");
        assertThat(mvcResult.getResolvedException().getCause()).isInstanceOf(InterruptedException.class);


        verify(tokenValidator).getServiceName("auth-header-value");
        verify(letterService).send(any(Letter.class), anyString());
        verifyNoMoreInteractions(tokenValidator, letterService);
    }

    @Test
    public void should_return_json_processing_exception_when_service_fails_to_serialize_letter() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("service-name");
        willThrow(JsonProcessingException.class).given(letterService).send(any(Letter.class), anyString());

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

        verify(letterService, never()).send(any(Letter.class), anyString());
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
