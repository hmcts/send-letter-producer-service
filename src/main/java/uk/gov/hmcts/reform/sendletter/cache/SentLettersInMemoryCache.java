package uk.gov.hmcts.reform.sendletter.cache;

import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Caches (successfully) sent letters in memory (no ttl).
 */
public class SentLettersInMemoryCache implements SentLettersCache {

    private final Set<String> letters;
    private final LetterChecksumGenerator checksumChecker;

    public SentLettersInMemoryCache(LetterChecksumGenerator checksumChecker) {
        this(checksumChecker, new HashSet<>());
    }

    public SentLettersInMemoryCache(LetterChecksumGenerator checksumChecker, Set<String> letters) {
        this.checksumChecker = checksumChecker;
        this.letters = letters;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean add(Letter letter) {
        return letters.add(checksum(letter));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void remove(Letter letter) {
        letters.remove(checksum(letter));
    }

    private String checksum(Letter letter) {
        return checksumChecker.generateChecksum(letter);
    }
}
