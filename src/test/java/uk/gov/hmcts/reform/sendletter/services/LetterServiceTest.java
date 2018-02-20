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
import org.springframework.dao.CleanupFailureDataAccessException;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.domain.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.ConnectionException;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.SendMessageException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintedAtPatch;
import uk.gov.hmcts.reform.sendletter.model.LetterSentToPrintAtPatch;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
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
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private LetterRepository letterRepository;

    @Before
    public void setUp() {
        service = new LetterService(queueClientSupplier, insights, objectMapper, 7, letterRepository);
        voidCompletableFuture = CompletableFuture.completedFuture(null);
        failedCompletableFuture = new CompletableFuture<>();
        failedCompletableFuture.completeExceptionally(new Exception("some exception"));
    }

    @Test
    public void should_return_letter_status_when_it_is_found_in_database() {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());
        LetterStatus status = new LetterStatus(UUID.randomUUID(), "some-message-id", now, now, now);

        given(letterRepository.getLetterStatus(status.id, "service-name")).willReturn(Optional.of(status));

        assertThat(service.getStatus(status.id, "service-name"))
            .isEqualToComparingFieldByField(status);
    }

    @Test
    public void should_throw_letter_not_found_exception_when_not_in_database() {
        UUID id = UUID.randomUUID();
        given(letterRepository.getLetterStatus(id, "service-name")).willReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> service.getStatus(id, "service-name"));

        assertThat(exception)
            .isInstanceOf(LetterNotFoundException.class)
            .hasMessage("Letter with ID '" + id.toString() + "' not found");
    }

    @Test
    public void should_save_report_and_return_message_id_when_single_letter_is_sent() throws Exception {
        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        given(queueClient.sendAsync(any(Message.class))).willReturn(voidCompletableFuture);
        given(queueClient.closeAsync()).willReturn(voidCompletableFuture);
        doNothing()
            .when(letterRepository)
            .save(any(DbLetter.class), any(Instant.class), anyString());

        //when
        UUID letterUuid = service.send(letter, "service");
        String letterId = letterUuid.toString();
        //then
        assertThat(letterId).isNotNull();

        verify(queueClientSupplier).get();
        verify(queueClient).send(any(Message.class));
        verify(letterRepository).save(any(DbLetter.class), any(Instant.class), anyString());

        voidCompletableFuture.thenRun(() -> {
            verify(queueClient).closeAsync();
            verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(true), eq(letterId));
            verifyNoMoreInteractions(queueClientSupplier, queueClient, insights, letterRepository);
        });
    }

    @Test
    public void should_not_save_report_and_send_message_when_saving_report_fails() throws Exception {
        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        willThrow(CleanupFailureDataAccessException.class).given(letterRepository)
            .save(any(DbLetter.class), any(Instant.class), anyString());

        //when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        //then
        assertThat(exception).isInstanceOf(CleanupFailureDataAccessException.class);

        verify(queueClientSupplier, never()).get();
        verify(letterRepository).save(any(DbLetter.class), any(Instant.class), anyString());
        verifyNoMoreInteractions(queueClientSupplier, letterRepository);
    }

    @Test
    public void should_throw_send_message_exception_when_single_letter_is_sent() throws Exception {
        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        willThrow(new RuntimeException("test exception")).given(queueClient).send(any(Message.class));

        //when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        //then
        assertThat(exception)
            .isInstanceOf(SendMessageException.class)
            .hasMessageStartingWith("Could not send message to ServiceBus");

        verify(queueClientSupplier).get();
        verify(queueClient).send(any(Message.class));

        failedCompletableFuture.thenRun(() -> {
            verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(false), anyString());
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
    public void should_rethrow_runtime_exception_if_message_sending_fails() throws Exception {
        RuntimeException thrownException = new RuntimeException("test exception");

        // given
        given(queueClientSupplier.get()).willReturn(queueClient);
        willThrow(thrownException).given(queueClient).send(any(Message.class));

        // when
        Throwable exception = catchThrowable(() -> service.send(letter, "service"));

        // then
        assertThat(exception)
            .isInstanceOf(RuntimeException.class);

        verify(queueClientSupplier).get();
        verify(queueClient).send(any(Message.class));
        verify(queueClient).close();
        verify(insights).trackMessageAcknowledgement(any(Duration.class), eq(false), anyString());
        verify(insights).trackException(thrownException);
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

    @Test
    public void update_should_throw_an_exception_if_no_letters_were_updated() {
        given(letterRepository.updateSentToPrintAt(any(), any())).willReturn(0);

        Throwable exc = catchThrowable(() -> {
            service.updateSentToPrintAt(
                UUID.randomUUID(),
                new LetterSentToPrintAtPatch(LocalDateTime.now())
            );
        });

        assertThat(exc).isInstanceOf(LetterNotFoundException.class);
    }

    @Test
    public void update_should_pass_correct_data_to_update_database() {
        given(letterRepository.updateSentToPrintAt(any(), any())).willReturn(1);
        UUID id = UUID.randomUUID();
        LocalDateTime sentToPrintAt = LocalDateTime.now();

        // when
        service.updateSentToPrintAt(id, new LetterSentToPrintAtPatch(sentToPrintAt));

        // then
        verify(letterRepository).updateSentToPrintAt(id, sentToPrintAt);
    }

    @Test
    public void updatePrintedAt_should_throw_an_exception_if_no_letters_were_updated() {
        given(letterRepository.updatePrintedAt(any(), any())).willReturn(0);

        Throwable exc = catchThrowable(() -> {
            service.updatePrintedAt(
                UUID.randomUUID(),
                new LetterPrintedAtPatch(LocalDateTime.now())
            );
        });

        assertThat(exc).isInstanceOf(LetterNotFoundException.class);
    }

    @Test
    public void updatePrintedAt_should_pass_correct_data_to_update_database() {
        given(letterRepository.updatePrintedAt(any(), any())).willReturn(1);
        UUID id = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.now();

        // when
        service.updatePrintedAt(id, new LetterPrintedAtPatch(dateTime));

        // then
        verify(letterRepository).updatePrintedAt(id, dateTime);
    }

    @Test
    public void updateIsFailed_should_throw_an_exception_if_no_letters_were_updated() {
        given(letterRepository.updateIsFailed(any())).willReturn(0);

        Throwable exc = catchThrowable(() -> {
            service.updateIsFailed(UUID.randomUUID());
        });

        assertThat(exc).isInstanceOf(LetterNotFoundException.class);
    }

    @Test
    public void updateIsFailed_should_pass_correct_data_to_update_database() {
        given(letterRepository.updateIsFailed(any())).willReturn(1);
        UUID id = UUID.randomUUID();
        // when
        service.updateIsFailed(id);

        // then
        verify(letterRepository).updateIsFailed(id);
    }
}
