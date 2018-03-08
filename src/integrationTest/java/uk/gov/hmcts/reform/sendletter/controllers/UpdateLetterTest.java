package uk.gov.hmcts.reform.sendletter.controllers;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.microsoft.azure.servicebus.IQueueClient;
import org.json.JSONObject;
import org.junit.Before;
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
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.UnauthorizedException;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;
import uk.gov.hmcts.reform.sendletter.services.AuthService;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
public class UpdateLetterTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private QueueClientSupplier queueClientSupplier;

    @Mock
    private IQueueClient queueClient;

    @SpyBean
    private AuthService authService;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @Before
    public void init() {
        given(queueClientSupplier.get()).willReturn(queueClient);
    }

    @Test
    public void should_return_204_when_is_failed_column_is_successfully_updated() throws Exception {
        //given
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("sendletterconsumer");

        String response = send(readResource("letter.json"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String letterId = new JSONObject(response).getString("letter_id");

        update(letterId + "/is-failed")
            .andExpect(status().is(204));

        verify(authService).assertCanUpdateLetter("sendletterconsumer");
    }

    @Test
    public void updateIsFailed_should_throw_letter_not_found_exception_when_letter_id_does_not_exists_in_db()
        throws Exception {
        //given
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("sendletterconsumer");

        //when
        MvcResult mvcResult = update(UUID.randomUUID() + "/is-failed")
            .andExpect(status().isNotFound())
            .andReturn();

        assertThat(mvcResult.getResolvedException())
            .isExactlyInstanceOf(LetterNotFoundException.class);

        verify(authService).assertCanUpdateLetter("sendletterconsumer");
    }

    @Test
    public void should_throw_unauthorized_exception_when_service_is_not_allowed_to_update_is_failed() throws Exception {
        //given
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("some-test-value");

        //when
        MvcResult mvcResult = update(UUID.randomUUID() + "/is-failed")
            .andExpect(status().isForbidden())
            .andReturn();

        assertThat(mvcResult.getResolvedException())
            .isExactlyInstanceOf(UnauthorizedException.class);

        verify(authService).assertCanUpdateLetter("some-test-value");
    }


    private ResultActions send(String content) throws Exception {
        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }

    private ResultActions update(String fieldPath) throws Exception {
        MockHttpServletRequestBuilder request =
            put("/letters/" + fieldPath)
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON);

        return mvc.perform(request);
    }

    private String readResource(final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charsets.UTF_8);
    }
}
