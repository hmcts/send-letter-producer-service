package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.Matchers.any;


@RunWith(MockitoJUnitRunner.class)
public class LetterServiceTest {

    private final Letter letter = SampleData.letter();

    private LetterService service;

    @Mock
    private IQueueClient queueClient;

    @Mock
    private ObjectMapper objectMapper;

    private CompletableFuture<Void> voidCompletableFuture;

    @Before
    public void setUp() {
        this.service = new LetterService(() -> queueClient, objectMapper, 7);
        voidCompletableFuture = CompletableFuture.completedFuture(null);
    }

    @Test
    public void should_return_message_id_when_single_letter_is_sent() throws Exception {
        // given
        given(queueClient.sendAsync(any(Message.class))).willReturn(voidCompletableFuture);

        //when
        String messageId = service.send(letter, "service");

        //then
        assertThat(messageId).isNotNull();
    }

    @Test
    public void should_throw_service_bus_exception_when_queue_client_fails_to_connect() throws Exception {
        // given
        given(queueClient.sendAsync(any(Message.class))).willThrow(ServiceBusException.class);

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(ServiceBusException.class);

        verify(queueClient).sendAsync(any(Message.class));
        verifyNoMoreInteractions(queueClient);
    }

    @Test
    public void should_throw_json_processing_exception_letter_serialization_fails() throws Exception {
        // given
        given(objectMapper.writeValueAsBytes(any())).willThrow(JsonProcessingException.class);

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    public void should_throw_interrupted_exception_when_letter_is_invalid() throws Exception {
        // given
        given(queueClient.sendAsync(any(Message.class))).willThrow(InterruptedException.class);

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(InterruptedException.class);

        verify(queueClient).sendAsync(any(Message.class));
        verifyNoMoreInteractions(queueClient);
    }


    @Test
    public void should_rethrow_runtime_exception_if_invocation_fails() throws Exception {
        // given
        given(queueClient.sendAsync(any(Message.class))).willThrow(RuntimeException.class);

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(RuntimeException.class);

        verify(queueClient).sendAsync(any(Message.class));
        verifyNoMoreInteractions(queueClient);
    }

    @Test
    public void should_not_allow_null_service_name() throws Exception {
        assertThatThrownBy(() -> service.send(letter, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_not_allow_empty_service_name() {
        assertThatThrownBy(() -> service.send(letter, ""))
            .isInstanceOf(IllegalStateException.class);
    }
}
