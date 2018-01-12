package uk.gov.hmcts.reform.sendletter.cache;

import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

import java.util.concurrent.TimeUnit;

/**
 * Stores (successfully) sent letters for predefined amount of time.
 */
@Component
public class SentLettersCache {

    private final RSetCache<String> redisCache;
    private final LetterChecksumGenerator checksumChecker;
    private final long ttlInSeconds;

    public SentLettersCache(
        RedissonClient redisClient,
        LetterChecksumGenerator checksumChecker,
        @Value("ttlInSeconds") long ttlInSeconds
    ) {
        this.redisCache = redisClient.getSetCache("sent_letters");
        this.checksumChecker = checksumChecker;
        this.ttlInSeconds = ttlInSeconds;
    }

    /**
     * Adds letter to cache.
     *
     * @return <code>true</code> if value has been added.
     * <code>false</code> if value already been in collection.
     */
    public boolean add(Letter letter) {
        return redisCache.add(
            checksum(letter),
            ttlInSeconds,
            TimeUnit.SECONDS
        );
    }

    public void remove(Letter letter) {
        redisCache.remove(checksum(letter));
    }

    private String checksum(Letter letter) {
        return checksumChecker.generateChecksum(letter);
    }
}
