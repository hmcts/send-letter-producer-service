package uk.gov.hmcts.reform.sendletter.notify;

import uk.gov.hmcts.reform.sendletter.model.Letter;

public interface INotifyClient {
    void send(Letter letter);
}
