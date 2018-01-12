package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.exceptions.LetterAlreadySentException;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.notify.INotifyClient;

@Service
public class LetterService {

    private final SentLettersCache cache;
    private final INotifyClient notifyClient;

    public LetterService(
        SentLettersCache cache,
        INotifyClient notifyClient
    ) {
        this.cache = cache;
        this.notifyClient = notifyClient;
    }

    public void send(Letter letter) {
        if (cache.add(letter)) {
            try {
                notifyClient.send(letter);
            } catch (Exception exc) {
                cache.remove(letter);
                throw exc;
            }
        } else {
            throw new LetterAlreadySentException();
        }
    }
}
