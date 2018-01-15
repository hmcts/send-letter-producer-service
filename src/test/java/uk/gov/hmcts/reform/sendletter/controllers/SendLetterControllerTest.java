package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class SendLetterControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private LetterService letterService; //NOPMD
    @MockBean private AuthTokenValidator validator;

    @Test
    public void should_return_401_when_auth_token_is_invalid() throws Exception {
        given(validator.getServiceName(anyString()))
            .willThrow(InvalidTokenException.class);

        mockMvc.perform(
            post("/letters")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "invalid token")
                .content("{ \"a\": \"b\"}")
        ).andExpect(
            status().isUnauthorized()
        );
    }
}
