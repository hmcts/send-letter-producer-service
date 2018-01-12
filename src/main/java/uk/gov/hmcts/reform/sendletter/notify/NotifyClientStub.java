package uk.gov.hmcts.reform.sendletter.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.Letter;

@Component
@ConditionalOnProperty(prefix = "notify", name = "stub", havingValue = "true")
public class NotifyClientStub implements INotifyClient {

    private static final Logger log = LoggerFactory.getLogger(NotifyClientStub.class);

    public NotifyClientStub() {
        log.info("USING STUBBED GOV NOTIFY CLIENT!");
    }

    @Override
    public void send(Letter letter) {
        log.info("Stubbed sending letter to notify");
    }
}
