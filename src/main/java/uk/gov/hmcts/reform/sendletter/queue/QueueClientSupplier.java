package uk.gov.hmcts.reform.sendletter.queue;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.ConnectionException;

import java.util.function.Supplier;

@Component
public class QueueClientSupplier implements Supplier<IQueueClient> {

    private final String connectionString;

    private final String queueName;

    public QueueClientSupplier(@Value("${servicebus.connectionString}") String connectionString,
                               @Value("${servicebus.queue.name}") String queueName) {
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    @Override
    public IQueueClient get() {
        try {
            return new QueueClient(
                new ConnectionStringBuilder(connectionString, queueName),
                ReceiveMode.PEEKLOCK
            );
        } catch (InterruptedException | ServiceBusException exception) {
            throw new ConnectionException("Unable to connect to Azure service bus", exception);
        }
    }
}
