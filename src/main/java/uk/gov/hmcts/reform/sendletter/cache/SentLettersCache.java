package uk.gov.hmcts.reform.sendletter.cache;

import uk.gov.hmcts.reform.sendletter.model.Letter;

public interface SentLettersCache {

    /**
     * Adds letter to cache.
     *
     * @param letter Letter
     *
     * @return <code>true</code> if value has been added.
     * <code>false</code> if value already been in collection.
     */
    boolean add(Letter letter);

    /**
     * Removes letter from cache.
     *
     * @param letter Letter
     */
    void remove(Letter letter);
}
