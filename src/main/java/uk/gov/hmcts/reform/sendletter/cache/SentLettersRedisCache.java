package uk.gov.hmcts.reform.sendletter.cache;

import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

import java.util.concurrent.TimeUnit;

/**
 * Stores (successfully) sent letters for predefined amount of time.
 */
public class SentLettersRedisCache implements SentLettersCache {

    private final RSetCache<String> redisCache;
    private final LetterChecksumGenerator checksumChecker;
    private final long ttlInSeconds;

    public SentLettersRedisCache(
        RedissonClient redisClient,
        LetterChecksumGenerator checksumChecker,
        long ttlInSeconds
    ) {
        this.redisCache = redisClient.getSetCache("sent_letters");
        this.checksumChecker = checksumChecker;
        this.ttlInSeconds = ttlInSeconds;
    }

    @Override
    public boolean add(Letter letter) {
        return redisCache.add(
            checksum(letter),
            ttlInSeconds,
            TimeUnit.SECONDS
        );
    }

    @Override
    public void remove(Letter letter) {
        redisCache.remove(checksum(letter));
    }

    private String checksum(Letter letter) {
        return checksumChecker.generateChecksum(letter);
    }
}
