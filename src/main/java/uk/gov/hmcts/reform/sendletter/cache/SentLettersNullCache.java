package uk.gov.hmcts.reform.sendletter.cache;

import uk.gov.hmcts.reform.sendletter.model.Letter;

/**
 * Cache implementation to not cache whatsoever. Addition is always a positive action
 */
public class SentLettersNullCache implements SentLettersCache {

    public SentLettersNullCache() {
        // empty constructor
    }

    @Override
    public boolean add(Letter letter) {
        return true;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void remove(Letter letter) {
        // nothing to remove
    }
}
