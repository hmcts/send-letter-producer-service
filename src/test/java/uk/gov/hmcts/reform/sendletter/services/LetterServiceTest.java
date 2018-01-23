package uk.gov.hmcts.reform.sendletter.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.cache.SentLettersCache;
import uk.gov.hmcts.reform.sendletter.exceptions.LetterAlreadySentException;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.notify.INotifyClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LetterServiceTest {

    @Mock private SentLettersCache cache;
    @Mock private INotifyClient notifyClient;

    private final Letter letter = SampleData.letter();

    private LetterService service;

    @Before
    public void setUp() {
        service = new LetterService(cache, notifyClient);
    }

    @Test
    public void should_add_checksum_to_cache_if_letter_was_sent() {

        // given
        cacheIsEmpty();

        // when
        service.send(letter);

        // then
        verify(cache, times(1)).add(letter);
        verify(cache, times(0)).remove(letter);
    }

    @Test
    public void should_send_a_request_to_notify_if_letter_was_not_sent_previously() {

        // given
        cacheIsEmpty();

        // when
        service.send(letter);

        // then
        verify(notifyClient, times(1)).send(letter);
    }

    @Test
    public void should_throw_exception_if_given_letter_was_sent_previously() {

        // given
        cacheContains(letter);

        // when
        Throwable exc = catchThrowable(() -> service.send(letter));

        // then
        assertThat(exc).isInstanceOf(LetterAlreadySentException.class);
    }

    @Test
    public void should_not_store_letter_checksum_if_notify_call_failed() {

        // given
        cacheIsEmpty();
        doThrow(Exception.class)
            .when(notifyClient)
            .send(any(Letter.class));

        // when
        catchThrowable(() -> service.send(letter));

        // then
        verify(cache, times(1)).add(letter);
        verify(cache, times(1)).remove(letter);
    }

    @Test
    public void should_rethrow_notify_exception_if_call_failed() {

        // given
        cacheIsEmpty();
        doThrow(new RuntimeException("foo!"))
            .when(notifyClient)
            .send(any(Letter.class));

        // when
        Throwable exc = catchThrowable(() -> service.send(letter));

        // then
        assertThat(exc)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("foo!");
    }

    private void cacheIsEmpty() {
        given(cache.add(any(Letter.class)))
            .willReturn(true);
    }

    private void cacheContains(Letter letter) {
        given(cache.add(letter))
            .willReturn(false);
    }
}
