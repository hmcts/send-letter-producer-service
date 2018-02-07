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
import uk.gov.hmcts.reform.sendletter.exception.SendMessageException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.WithServiceName;

import java.time.Duration;
import java.time.Instant;
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

    public LetterService(Supplier<IQueueClient> queueClientSupplier,
                         AppInsights insights,
                         ObjectMapper objectMapper,
                         @Value("${servicebus.queue.messageTTLInDays}") int messageTimeToLive) {
        this.queueClientSupplier = queueClientSupplier;
        this.insights = insights;
        this.objectMapper = objectMapper;
        this.messageTimeToLive = messageTimeToLive;
    }

    public String send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        IQueueClient sendClient = queueClientSupplier.get();

        final String messageId = generateChecksum(letter);

        log.info("Generated message: id = {} for letter with print queue id = {} ", messageId, letter.type);

        Message message = createQueueMessage(new WithServiceName<>(letter, serviceName), messageId);
        Instant startSending = Instant.now();

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

    private Message createQueueMessage(WithServiceName<Letter> letter, String messageId)
        throws JsonProcessingException {

        insights.trackMessageReceived(letter.service, letter.obj.template, messageId);

        Message message = new Message(objectMapper.writeValueAsBytes(letter));

        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTimeToLive));

        return message;
    }
}
