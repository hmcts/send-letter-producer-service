package uk.gov.hmcts.reform.sendletter.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class SentLettersInMemoryCacheTest {

    @Mock private LetterChecksumGenerator checksumGen;

    private final Set<String> set = new HashSet<>();
    private SentLettersCache cache;

    @Before
    public void setUp() {
        cache = new SentLettersInMemoryCache(checksumGen, set);
    }

    @Test
    public void should_use_letter_checksum_as_value() {
        // given
        String checksum = "abcd1234";
        given(checksumGen.generateChecksum(any())).willReturn(checksum);

        // when
        boolean added = cache.add(new Letter());

        // then
        assertThat(added).isTrue();
        assertThat(set).containsExactly(checksum);
    }

    @Test
    public void add_should_return_false_if_letter_was_already_added_earlier() {
        // given
        Letter letter = new Letter();
        String checksum = "abcd1234";
        given(checksumGen.generateChecksum(any())).willReturn(checksum);

        // when the same letter is added twice
        cache.add(letter);
        boolean secondResp = cache.add(letter);

        // then
        assertThat(secondResp).isFalse();
        assertThat(set).containsExactly(checksum);
    }

    @Test
    public void should_remove_letter_from_set() {
        // given
        Letter letter = new Letter();
        given(checksumGen.generateChecksum(any())).willReturn("abcd1234");

        // when
        cache.add(letter);
        cache.remove(letter);

        // then
        assertThat(set).isEmpty();
    }
}
