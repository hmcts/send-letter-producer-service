package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.exception.UnauthorizedException;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class CheckPrintStatusControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private LetterService letterService;
    @MockBean private AuthService authService;

    @Before
    public void setUp() {
        given(authService.authenticate("auth-header-value")).willReturn("service-name");
    }

    @After
    public void tearDown() {
        reset(letterService);
    }

    @Test
    public void should_return_204_when_requested_to_check_print_status() throws Exception {
        printState().andExpect(status().isNoContent());

        verify(letterService).checkPrintState();
    }

    @Test
    public void should_return_403_when_service_is_not_allowed_to_access_endpoint() throws Exception {
        willThrow(UnauthorizedException.class).given(authService).assertCanCheckStatus("service-name");

        printState().andExpect(status().isForbidden());
    }

    @Test
    public void should_return_401_if_service_auth_header_is_missing() throws Exception {
        reset(authService);
        given(authService.authenticate(null)).willThrow(new UnauthenticatedException("Hello"));

        mockMvc.perform(post("/letter-reports/print-status-check"))
            .andExpect(status().isUnauthorized());
    }

    private ResultActions printState() throws Exception {
        return mockMvc.perform(
            post("/letter-reports/print-status-check")
                .header("ServiceAuthorization", "auth-header-value")
        );
    }
}
