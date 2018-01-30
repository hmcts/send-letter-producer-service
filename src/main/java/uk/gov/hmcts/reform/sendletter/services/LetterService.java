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
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.WithServiceName;

import java.time.Duration;
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final Supplier<IQueueClient> queueClientSupplier;

    private final ObjectMapper objectMapper;


    private final int messageTimeToLive;

    public LetterService(Supplier<IQueueClient> queueClientSupplier,
                         ObjectMapper objectMapper,
                         @Value("${servicebus.queue.messageTTLInDays}") int messageTimeToLive) {
        this.queueClientSupplier = queueClientSupplier;
        this.objectMapper = objectMapper;
        this.messageTimeToLive = messageTimeToLive;
    }

    public String send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        IQueueClient sendClient = queueClientSupplier.get();

        final String messageId = generateChecksum(letter);

        log.info("Generated message: id = {} for letter with print queue id = {} ", messageId, letter.type);

        sendClient
            .sendAsync(createQueueMessage(new WithServiceName<>(letter, serviceName), messageId))
            .thenRunAsync(() -> {
                log.info("Message acknowledged: id = {}", messageId);
            })
            .thenRunAsync(sendClient::closeAsync);

        return messageId;
    }

    private Message createQueueMessage(WithServiceName<Letter> letter, String messageId)
        throws JsonProcessingException {

        Message message = new Message(objectMapper.writeValueAsBytes(letter));
        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTimeToLive));
        return message;
    }
}
