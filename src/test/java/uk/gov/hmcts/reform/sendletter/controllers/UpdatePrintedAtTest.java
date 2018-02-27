package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class UpdatePrintedAtTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private LetterService letterService; //NOPMD
    @MockBean private AuthService authService;

    @Test
    public void should_return_204_on_successful_update() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some-service-name");

        mockMvc.perform(
            put("/letters/" + UUID.randomUUID().toString() + "/printed-at")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("ServiceAuthorization", "auth-header-value")
                .content("{\"printed_at\": \"2018-02-14T09:32:15Z\"}")
        ).andExpect(status().isNoContent());
    }

    @Test
    public void should_validate_service_token() throws Exception {

        final String serviceToken = "my_service_token";

        mockMvc.perform(
            put("/letters/" + UUID.randomUUID().toString() + "/printed-at")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("ServiceAuthorization", serviceToken)
                .content("{\"printed_at\": \"2018-02-14T09:32:15Z\"}")
        );

        verify(authService).authenticate(serviceToken);
        verify(authService).assertCanUpdateLetter(anyString());
    }
}
