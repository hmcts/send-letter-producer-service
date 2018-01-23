package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.notify.INotifyClient;

@Service
public class LetterService {

    private final INotifyClient notifyClient;

    public LetterService(INotifyClient notifyClient) {
        this.notifyClient = notifyClient;
    }

    public void send(Letter letter) {
        notifyClient.send(letter);
    }
}
