package uk.gov.hmcts.reform.sendletter.health;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import uk.gov.hmcts.reform.sendletter.queue.QueueClientSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;


@RunWith(MockitoJUnitRunner.class)
public class ServiceBusHealthIndicatorTest {

    @Mock
    private QueueClientSupplier clientSupplier;

    @Test
    public void should_return_up_when_connection_with_the_queue_can_be_established() {
        given(clientSupplier.get()).willReturn(mock(IQueueClient.class));

        Health health = new ServiceBusHealthIndicator(clientSupplier).health();

        assertThat(health.getStatus()).isEqualTo(UP);
    }

    @Test
    public void should_return_down_when_connection_with_the_queue_cannot_be_established() {
        doThrow(ServiceBusException.class).when(clientSupplier).get();

        Health health = new ServiceBusHealthIndicator(clientSupplier).health();

        assertThat(health.getStatus()).isEqualTo(DOWN);
    }
}
