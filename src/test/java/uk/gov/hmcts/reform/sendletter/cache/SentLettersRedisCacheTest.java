package uk.gov.hmcts.reform.sendletter.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SentLettersRedisCacheTest {

    private @Mock RedissonClient redissonClient;
    private @Mock RSetCache<String> redisCache;
    private @Mock LetterChecksumGenerator checksumGenerator;

    @Before
    public void setUp() {
        redissonClient = mock(RedissonClient.class);
        given(redissonClient.<String>getSetCache(anyString())).willReturn(redisCache);
    }

    @Test
    public void should_use_letter_checksum_as_value() {
        // given
        SentLettersCache cache = new SentLettersRedisCache(redissonClient, checksumGenerator, 123);

        given(checksumGenerator.generateChecksum(any(Letter.class)))
            .willReturn("abcd1234");

        // when
        cache.add(new Letter());

        // then
        verify(redisCache).add(eq("abcd1234"), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void should_set_ttl_for_specified_number_of_seconds() {
        // given
        long ttlInSeconds = 3_600;
        SentLettersCache cache = new SentLettersRedisCache(redissonClient, checksumGenerator, ttlInSeconds);

        // when
        cache.add(sampleLetter());

        // then
        verify(redisCache).add(anyString(), eq(ttlInSeconds), eq(TimeUnit.SECONDS));
    }

    @Test
    public void should_use_letter_checksum_when_removing_items_from_cache() {
        // given
        SentLettersCache cache = new SentLettersRedisCache(redissonClient, checksumGenerator, 123);

        given(checksumGenerator.generateChecksum(any(Letter.class))).willReturn("abcd1234");

        // when
        cache.remove(new Letter());

        // then
        verify(redisCache).remove("abcd1234");
    }

    private Letter sampleLetter() {
        return new Letter();
    }
}
