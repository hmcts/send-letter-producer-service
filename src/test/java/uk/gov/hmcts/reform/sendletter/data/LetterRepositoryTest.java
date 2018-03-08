package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.NotPrintedLetter;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class LetterRepositoryTest {

    private final Letter letter = SampleData.letter();

    private LetterRepository letterRepository;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource repository;

    @Before
    public void setUp() {
        jdbcTemplate = new NamedParameterJdbcTemplate(repository);
        letterRepository = new LetterRepository(jdbcTemplate, objectMapper);
    }

    @Test
    public void should_successfully_save_report_in_db() throws JsonProcessingException {
        //given
        DbLetter dbLetter = new DbLetter(UUID.randomUUID(), "cmc", letter);

        //when
        letterRepository.save(dbLetter, Instant.now(), UUID.randomUUID().toString());
        LetterStatus status = letterRepository.getLetterStatus(dbLetter.id, dbLetter.service).get();
        assertThat(status.id).isEqualTo(dbLetter.id);
    }

    @Test
    public void should_return_empty_optional_case_when_no_letters_found() {
        Optional<LetterStatus> result = letterRepository.getLetterStatus(UUID.randomUUID(), "some-service");

        assertThat(result.orElse(null)).isNull();
    }
}
