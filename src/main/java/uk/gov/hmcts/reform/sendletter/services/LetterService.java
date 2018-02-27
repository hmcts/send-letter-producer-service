package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.SendMessageException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.in.LetterPrintedAtPatch;
import uk.gov.hmcts.reform.sendletter.model.in.LetterSentToPrintAtPatch;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.queue.model.QueueLetter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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
        return letterRepository
            .getLetterStatus(id, serviceName)
            .orElseThrow(() -> new LetterNotFoundException(id));
    }

    @Transactional
    public UUID send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        final String messageId = generateChecksum(letter);
        final UUID id = UUID.randomUUID();

        log.info("Generated message: id = {} for letter with print queue id = {} and letter id = {} ",
            messageId,
            letter.type,
            id);

        DbLetter dbLetter = new DbLetter(id, serviceName, letter);

        //Save message details to db for reporting
        letterRepository.save(dbLetter, Instant.now(), messageId);
        placeLetterInQueue(dbLetter, messageId);
        return id;
    }

    private void placeLetterInQueue(
        DbLetter dbLetter,
        String messageId
    ) throws JsonProcessingException {

        Instant sendingStartTime = Instant.now();
        Message message = createQueueMessage(dbLetter, messageId);
        IQueueClient sendClient = queueClientSupplier.get();

        try {
            sendClient.send(message);
            logMessageSendCompletion(sendingStartTime, messageId, null);
        } catch (Exception exc) {
            logMessageSendCompletion(sendingStartTime, messageId, exc);

            throw new SendMessageException(
                String.format("Could not send message to ServiceBus. Message ID: %s", messageId),
                exc
            );
        } finally {
            try {
                if (sendClient != null) {
                    sendClient.close();
                }
            } catch (ServiceBusException exc) {
                log.error("Failed to close the queue client", exc);
            }
        }
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

    @Transactional
    public void updateIsFailed(UUID id) {
        int numberOfUpdatedRows = letterRepository.updateIsFailed(id);
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

    private Message createQueueMessage(DbLetter dbLetter, String messageId) throws JsonProcessingException {

        Message message = new Message(
            objectMapper.writeValueAsBytes(
                new QueueLetter(
                    dbLetter.id,
                    dbLetter.documents,
                    dbLetter.type,
                    dbLetter.service
                )
            )
        );

        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTimeToLive));

        return message;
    }
}
