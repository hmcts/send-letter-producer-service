package uk.gov.hmcts.reform.sendletter.cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class SentLettersNullCacheTest {

    @Test
    public void should_skip_the_addition_and_return_true() {
        // given
        SentLettersCache cache = new SentLettersNullCache();

        // when
        boolean added = cache.add(new Letter());

        // then
        assertThat(added).isTrue();
    }
}
