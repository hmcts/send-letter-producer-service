package uk.gov.hmcts.reform.sendletter.controllers;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.sendletter.FunSuite;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;

import java.util.concurrent.CompletableFuture;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@SpringBootTest
public class SendLetterTest extends FunSuite {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private QueueClientSupplier queueClientSupplier;

    @Mock
    private IQueueClient queueClient;

    private final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.completedFuture(null);

    private static final String LETTER_JSON = "{"
        + "\"template\": \"abc\","
        + "\"values\": { \"a\": \"b\" },"
        + "\"type\": \"typeA\""
        + "}";

    @Test
    public void should_return_200_when_single_letter_is_sent() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);
        given(queueClient.sendAsync(any(Message.class))).willReturn(voidCompletableFuture);

        send(LETTER_JSON)
            .andExpect(status().isOk());
    }

    @Test
    public void should_return_500_when_sending_message_has_failed() throws Exception {
        given(queueClientSupplier.get()).willReturn(queueClient);
        given(queueClient.sendAsync(any(Message.class))).willThrow(ServiceBusException.class);

        send(LETTER_JSON)
            .andExpect(status().isInternalServerError());
    }

    @Test
    public void should_return_400_when_bad_letter_is_sent() throws Exception {
        send("")
            .andExpect(status().isBadRequest());
    }

    private ResultActions send(String content) throws Exception {
        MockHttpServletRequestBuilder request =
            post("/letters")
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);

        return mvc.perform(request);
    }
}
