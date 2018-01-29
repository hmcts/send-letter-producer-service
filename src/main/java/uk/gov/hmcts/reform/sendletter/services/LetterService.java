package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;

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

    public String send(Letter letter) throws ServiceBusException, InterruptedException, JsonProcessingException {
        IQueueClient sendClient = queueClientSupplier.get();

        //TODO: Replace random int generation with checksum code
        final String messageId = Integer.toString(new Random().nextInt(100));

        log.info("Generated message: id = {} for letter with print queue id = {} ", messageId, letter.type);

        sendClient
            .sendAsync(createQueueMessage(letter, messageId))
            .thenRunAsync(() -> {
                log.info("Message acknowledged: id = {}", messageId);
            })
            .thenRunAsync(sendClient::closeAsync);

        return messageId;
    }

    private Message createQueueMessage(Letter letter, String messageId) throws JsonProcessingException {
        Message message = new Message(objectMapper.writeValueAsBytes(letter));
        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTimeToLive));
        return message;
    }
}
