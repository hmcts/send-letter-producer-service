package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
