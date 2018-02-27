package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.sendletter.exception.UnauthorizedException;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class UpdateIsFailedTest {
    @Autowired private MockMvc mockMvc;

    @MockBean private LetterService letterService; //NOPMD
    @MockBean private AuthService authService;

    @Test
    public void should_return_204_on_successful_update() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some-service-name");

        mockMvc.perform(
            put("/letters/" + UUID.randomUUID().toString() + "/is-failed")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("ServiceAuthorization", "auth-header-value")
        ).andExpect(status().isNoContent());

        verify(authService).authenticate(anyString());
        verify(authService).assertCanUpdateLetter(anyString());
    }

    @Test
    public void should_throw_unauthorized_exception_when_service_is_not_allowed_to_update_is_failed() throws Exception {
        given(authService.authenticate(anyString())).willReturn("some-service-name");
        willThrow(UnauthorizedException.class).given(authService).assertCanUpdateLetter("some-service-name");

        MvcResult mvcResult = mockMvc.perform(
            put("/letters/" + UUID.randomUUID().toString() + "/is-failed")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .header("ServiceAuthorization", "auth-header-value")
        ).andExpect(status().isForbidden())
            .andReturn();

        assertThat(mvcResult.getResolvedException())
            .isExactlyInstanceOf(UnauthorizedException.class);

        verify(authService).authenticate(anyString());
    }
}
