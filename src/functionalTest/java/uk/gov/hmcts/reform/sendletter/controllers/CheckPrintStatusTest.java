package uk.gov.hmcts.reform.sendletter.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
public class CheckPrintStatusTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @SpyBean
    private AppInsights insights; // NOPMD will be used with service implementation

    @SpyBean
    private LetterRepository letterRepository; // NOPMD will be used with service implementation

    @Test
    public void should_return_204_when_triggered_print_status_check_endpoint() throws Exception {
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("sendletterconsumer");

        ResultActions result = mvc.perform(post("/letter-reports/print-status-check")
            .header("ServiceAuthorization", "auth-header-value")
        );

        result.andExpect(status().isNoContent());

        // TODO
        // verify(letterRepository);
        // verify(insights);
    }
}
