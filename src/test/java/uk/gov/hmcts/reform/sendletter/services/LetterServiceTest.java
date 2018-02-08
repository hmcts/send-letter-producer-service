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
import uk.gov.hmcts.reform.sendletter.exception.ConnectionException;
import uk.gov.hmcts.reform.sendletter.exception.SendMessageException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class LetterServiceTest {

    private final Letter letter = SampleData.letter();

    private LetterService service;

    @Mock
    private ObjectMapper objectMapper;

    private CompletableFuture<Void> voidCompletableFuture;

    private CompletableFuture<Void> failedCompletableFuture;

    @Mock
    private Supplier<IQueueClient> queueClientSupplier;

    @Mock
    private AppInsights insights;

    @Mock
    private IQueueClient queueClient;

    @Before
    public void setUp() {
        service = new LetterService(queueClientSupplier, insights, objectMapper, 7);
        voidCompletableFuture = CompletableFuture.completedFuture(null);
        failedCompletableFuture = new CompletableFuture<>();
        failedCompletableFuture.completeExceptionally(new Exception("some exception"));
    }

    @Test
    public void should_return_message_id_when_single_letter_is_sent() throws Exception {
        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        given(queueClient.sendAsync(any(Message.class))).willReturn(voidCompletableFuture);
        given(queueClient.closeAsync()).willReturn(voidCompletableFuture);

        //when
        String messageId = service.send(letter, "service");

        //then
        assertThat(messageId).isNotNull();

        verify(queueClientSupplier).get();
        verify(queueClient).sendAsync(any(Message.class));

        voidCompletableFuture.thenRun(() -> {
            verify(queueClient).closeAsync();
            verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(true), eq(messageId));
            verify(insights).trackMessageReceived("service", letter.documents.get(0).template, messageId);
            verifyNoMoreInteractions(queueClientSupplier, queueClient, insights);
        });
    }

    @Test
    public void should_throw_send_message_exception_when_single_letter_is_sent() {
        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        given(queueClient.sendAsync(any(Message.class))).willReturn(failedCompletableFuture);
        given(queueClient.closeAsync()).willReturn(voidCompletableFuture);

        //when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        //then
        assertThat(exception)
            .isInstanceOf(SendMessageException.class)
            .hasMessageStartingWith("Could not send message to ServiceBus");

        verify(queueClientSupplier).get();
        verify(queueClient).sendAsync(any(Message.class));

        failedCompletableFuture.thenRun(() -> {
            verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(false), anyString());
            verify(insights).trackMessageReceived("service", letter.documents.get(0).template, anyString());
        });
        voidCompletableFuture.thenRun(() -> {
            verify(queueClient).closeAsync();
            verifyNoMoreInteractions(queueClientSupplier, queueClient, insights);
        });
    }

    @Test
    public void should_throw_connection_exception_when_queue_client_fails_to_connect() {
        // given
        given(queueClientSupplier.get())
            .willThrow(
                new ConnectionException("Unable to connect to Azure service bus",
                    new ServiceBusException(false))
            );

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(ConnectionException.class)
            .hasCauseExactlyInstanceOf(ServiceBusException.class)
            .hasMessage("Unable to connect to Azure service bus");

        verify(queueClientSupplier).get();
        verifyNoMoreInteractions(queueClientSupplier, queueClient, insights);
    }

    @Test
    public void should_throw_json_processing_exception_letter_serialization_fails() throws Exception {
        // given
        willThrow(JsonProcessingException.class).given(objectMapper).writeValueAsBytes(any());

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception).isInstanceOf(JsonProcessingException.class);
        verify(insights, never()).trackMessageAcknowledgement(any(Duration.class), anyBoolean(), anyString());
        verify(insights).trackMessageReceived(eq("service"), eq(letter.documents.get(0).template), anyString());
        verifyNoMoreInteractions(insights);
    }

    @Test
    public void should_throw_connection_exception_when_thread_is_interrupted() {
        // given
        given(queueClientSupplier.get())
            .willThrow(
                new ConnectionException("Unable to connect to Azure service bus",
                    new InterruptedException())
            );

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(ConnectionException.class)
            .hasCauseExactlyInstanceOf(InterruptedException.class)
            .hasMessage("Unable to connect to Azure service bus");


        verify(queueClientSupplier).get();
        verifyNoMoreInteractions(queueClientSupplier, queueClient, insights);
    }


    @Test
    public void should_rethrow_runtime_exception_if_invocation_fails() {
        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        willThrow(RuntimeException.class).given(queueClient).sendAsync(any(Message.class));

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(RuntimeException.class);

        verify(queueClientSupplier).get();
        verify(queueClient).sendAsync(any(Message.class));
        verify(insights, never()).trackMessageAcknowledgement(any(Duration.class), anyBoolean(), anyString());
        verify(insights).trackMessageReceived(eq("service"), eq(letter.documents.get(0).template), anyString());
        verifyNoMoreInteractions(queueClientSupplier, queueClient, insights);
    }

    @Test
    public void should_not_allow_null_service_name() {
        assertThatThrownBy(() -> service.send(letter, null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_not_allow_empty_service_name() {
        assertThatThrownBy(() -> service.send(letter, ""))
            .isInstanceOf(IllegalStateException.class);
    }
}
