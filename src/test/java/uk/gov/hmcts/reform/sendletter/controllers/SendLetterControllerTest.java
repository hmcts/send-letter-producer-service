package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

        sendValidLetter()
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void should_return_200_if_letter_is_valid() throws Exception {
        sendValidLetter()
            .andExpect(status().isOk());
    }

    @Test
    public void should_return_400_if_letter_is_invalid() throws Exception {
        sendLetterWithoutType()
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("errors")));
    }

    private ResultActions sendValidLetter() throws Exception {

        return sendLetter("{"
            + "\"template\": \"abc\","
            + "\"values\": { \"a\": \"b\" },"
            + "\"type\": \"typeA\""
            + "}");
    }

    private ResultActions sendLetterWithoutType() throws Exception {

        return sendLetter("{"
            + "\"template\": \"abc\","
            + "\"values\": { \"a\": \"b\" }"
            + "}");
    }

    private ResultActions sendLetter(String json) throws Exception {
        return mockMvc.perform(
            post("/letters")
                .contentType(MediaType.APPLICATION_JSON)
                .header("ServiceAuthorization", "invalid token")
                .content(json)
        );
    }
}
