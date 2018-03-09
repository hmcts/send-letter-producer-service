package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javassist.tools.rmi.Sample;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class LetterTest {

    @Autowired
    private LetterRepository repository;

    @Autowired
    private DataSource dataSource;

    @Test
    public void should_successfully_save_report_in_db() {
        Letter l = new Letter("messageId", "service", "{}", "a type");
        repository.save(l);
        int count = (int) repository.count();
        assertThat(count).isEqualTo(1);
    }
    @Test
    public void compatible_with_existing_records() throws JsonProcessingException {
        // Save a letter using the existing repository code.
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        ObjectMapper objectMapper = new ObjectMapper();
        uk.gov.hmcts.reform.sendletter.data.LetterRepository repo = new uk.gov.hmcts.reform.sendletter.data.LetterRepository(jdbcTemplate, objectMapper);
        DbLetter dbLetter = new DbLetter(UUID.randomUUID(), "cmc", SampleData.letter());
        Instant instant = Instant.now();
        String messageId = UUID.randomUUID().toString();
        repo.save(dbLetter, instant, messageId);

        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(1);

        Letter loaded = letters.get(0);
        String expectedData = objectMapper.writeValueAsString(SampleData.letter().additionalData);
        assertThat(loaded.additionalData).isEqualTo(expectedData);
        assertThat(loaded.createdAt).isEqualTo(Timestamp.from(instant));
        assertThat(loaded.messageId).isEqualTo(messageId);
        assertThat(loaded.service).isEqualTo("cmc");
        assertThat(loaded.type).isEqualTo(dbLetter.type);
    }
}
