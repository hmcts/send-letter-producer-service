package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.Letter;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class LetterRepositoryTest {

    private final Letter letter = SampleData.letter();

    private LetterRepository letterRepository;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        letterRepository = new LetterRepository(jdbcTemplate, objectMapper);
    }

    @Test
    public void should_successfully_save_report_in_db() throws JsonProcessingException {
        //given
        DbLetter dbLetter = new DbLetter(UUID.randomUUID(), "cmc", letter);

        given(jdbcTemplate.update(Mockito.anyString(), Mockito.any(MapSqlParameterSource.class)))
            .willReturn(1234);
        given(objectMapper.writeValueAsString(letter.additionalData))
            .willReturn("{\"caseId\":\"1234\",\"type\":\"test\"}");

        //when
        letterRepository.save(dbLetter, Instant.now(), UUID.randomUUID().toString());

        //then
        //No exception thrown and successfully saved in database
        verify(jdbcTemplate).update(Mockito.anyString(), Mockito.any(MapSqlParameterSource.class));
        verify(objectMapper).writeValueAsString(letter.additionalData);
        verifyNoMoreInteractions(jdbcTemplate, objectMapper);
    }

    @Test
    public void should_throw_json_processing_exception_when_additional_data_cannot_be_converted_to_json()
        throws JsonProcessingException {
        //given
        DbLetter dbLetter = new DbLetter(UUID.randomUUID(), "cmc", letter);

        given(jdbcTemplate.update(Mockito.anyString(), Mockito.any(MapSqlParameterSource.class)))
            .willReturn(1234);

        willThrow(JsonProcessingException.class).given(objectMapper).writeValueAsString(letter.additionalData);

        // when
        Throwable exception = catchThrowable(() ->
            letterRepository.save(dbLetter, Instant.now(), UUID.randomUUID().toString())
        );

        // then
        assertThat(exception).isInstanceOf(JsonProcessingException.class);

        verify(objectMapper).writeValueAsString(letter.additionalData);
    }

    @Test
    public void should_throw_sql_exception_and_not_save_report_in_db_when_database_connection_fails() {
        //given
        DbLetter dbLetter = new DbLetter(UUID.randomUUID(), "cmc", letter);

        given(jdbcTemplate.update(Mockito.anyString(), Mockito.any(MapSqlParameterSource.class)))
            .willReturn(1234);

        willThrow(SQLException.class)
            .given(jdbcTemplate)
            .update(Mockito.anyString(), Mockito.any(MapSqlParameterSource.class));

        // when
        Throwable exception = catchThrowable(() ->
            letterRepository.save(dbLetter, Instant.now(), UUID.randomUUID().toString())
        );

        // then
        assertThat(exception).isInstanceOf(SQLException.class);

        verify(jdbcTemplate).update(Mockito.anyString(), Mockito.any(MapSqlParameterSource.class));
        verifyNoMoreInteractions(jdbcTemplate);
    }
}
