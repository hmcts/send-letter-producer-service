package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.domain.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.SendMessageException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintedAtPatch;
import uk.gov.hmcts.reform.sendletter.model.LetterSentToPrintAtPatch;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final Supplier<IQueueClient> queueClientSupplier;

    private final ObjectMapper objectMapper;

    private final AppInsights insights;

    private final int messageTimeToLive;

    private final LetterRepository letterRepository;

    public LetterService(Supplier<IQueueClient> queueClientSupplier,
                         AppInsights insights,
                         ObjectMapper objectMapper,
                         @Value("${servicebus.queue.messageTTLInDays}") int messageTimeToLive,
                         LetterRepository letterRepository) {
        this.queueClientSupplier = queueClientSupplier;
        this.insights = insights;
        this.objectMapper = objectMapper;
        this.messageTimeToLive = messageTimeToLive;
        this.letterRepository = letterRepository;
    }

    public LetterStatus getStatus(UUID id, String serviceName) throws LetterNotFoundException {
        // TODO implement dao call

        return new LetterStatus(id);
    }

    @Transactional
    public UUID send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        IQueueClient sendClient = queueClientSupplier.get();

        final String messageId = generateChecksum(letter);

        final UUID id = UUID.randomUUID();

        log.info("Generated message: id = {} for letter with print queue id = {} and letter id = {} ",
            messageId,
            letter.type,
            id);

        DbLetter dbLetter = new DbLetter(id, serviceName, letter);

        Message message = createQueueMessage(dbLetter, messageId);

        Instant startSending = Instant.now();

        //Save message details to db for reporting
        letterRepository.save(dbLetter, startSending, messageId);

        CompletableFuture<Void> sendResult = sendClient.sendAsync(message);

        sendResult.whenCompleteAsync((result, exc) ->
            logMessageSendCompletion(startSending, messageId, exc)
        ).thenRunAsync(sendClient::closeAsync);

        if (sendResult.isCompletedExceptionally()) {
            throw new SendMessageException("Could not send message to ServiceBus with " + messageId);
        }

        return id;
    }

    @Transactional
    public void updateSentToPrintAt(UUID id, LetterSentToPrintAtPatch patch) {
        int numberOfUpdatedRows = letterRepository.updateSentToPrintAt(id, patch.sentToPrintAt);
        if (numberOfUpdatedRows == 0) {
            throw new LetterNotFoundException(id);
        }
    }

    @Transactional
    public void updatePrintedAt(UUID id, LetterPrintedAtPatch patch) {
        int numberOfUpdatedRows = letterRepository.updatePrintedAt(id, patch.printedAt);
        if (numberOfUpdatedRows == 0) {
            throw new LetterNotFoundException(id);
        }
    }

    private void logMessageSendCompletion(Instant started, String messageId, Throwable exception) {
        Duration tookSending = Duration.between(started, Instant.now());
        boolean hasFailed = exception != null;

        insights.trackMessageAcknowledgement(tookSending, !hasFailed, messageId);

        if (hasFailed) {
            insights.trackException((Exception) exception);
        } else {
            log.info("Message acknowledged: id = {}", messageId);
        }
    }

    private Message createQueueMessage(DbLetter letter, String messageId)
        throws JsonProcessingException {

        letter.documents
            .forEach(document ->
                insights.trackMessageReceived(letter.service, document.template, messageId));

        Message message = new Message(objectMapper.writeValueAsBytes(letter));

        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTimeToLive));

        return message;
    }
}
