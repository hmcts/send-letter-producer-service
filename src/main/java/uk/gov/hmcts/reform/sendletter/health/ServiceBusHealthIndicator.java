package uk.gov.hmcts.reform.sendletter.health;

import com.microsoft.azure.servicebus.IQueueClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ServiceBusHealthIndicator implements HealthIndicator {

    private final Supplier<IQueueClient> queueClientSupplier;

    public ServiceBusHealthIndicator(Supplier<IQueueClient> queueClientSupplier) {
        this.queueClientSupplier = queueClientSupplier;
    }

    @Override
    public Health health() {
        try {
            queueClientSupplier.get().close();
            return Health.up().build();
        } catch (Exception exc) {
            return Health.down().withException(exc).build();
        }
    }
}
