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
import uk.gov.hmcts.reform.sendletter.exception.SendMessageException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.WithServiceNameAndId;

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

    @Transactional
    public String send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        IQueueClient sendClient = queueClientSupplier.get();

        final String messageId = generateChecksum(letter);

        final UUID id = UUID.randomUUID();

        log.info("Generated message: id = {} for letter with print queue id = {} and letter id = {} ",
            messageId,
            letter.type,
            id);

        WithServiceNameAndId<Letter> letterWithServiceNameAndId = new WithServiceNameAndId<>(letter, serviceName, id);

        Message message = createQueueMessage(letterWithServiceNameAndId, messageId);

        Instant startSending = Instant.now();

        //Save message details to db for reporting
        letterRepository.save(letterWithServiceNameAndId, startSending, messageId);

        CompletableFuture<Void> sendResult = sendClient.sendAsync(message);

        sendResult.whenCompleteAsync((result, exc) ->
            logMessageSendCompletion(startSending, messageId, exc)
        ).thenRunAsync(sendClient::closeAsync);

        if (sendResult.isCompletedExceptionally()) {
            throw new SendMessageException("Could not send message to ServiceBus with " + messageId);
        }

        return messageId;
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

    private Message createQueueMessage(WithServiceNameAndId<Letter> letter, String messageId)
        throws JsonProcessingException {

        letter.obj.documents
            .forEach(document ->
                insights.trackMessageReceived(letter.service, document.template, messageId));

        Message message = new Message(objectMapper.writeValueAsBytes(letter));

        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTimeToLive));

        return message;
    }
}
