package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class LetterTest {

    @Autowired
    private LetterRepository repository;

    @Autowired
    private DataSource dataSource;

    public static final Letter testLetter = new Letter("messageId",
        "service", "{}", "a type", new byte[1]);

    @Test
    public void should_successfully_save_report_in_db() {
        repository.save(testLetter);
        int count = (int) repository.count();
        List<Letter> letters = Lists.newArrayList(repository.findAll());
        assertThat(letters.size()).isEqualTo(1);
        assertThat(letters.get(0).state).isEqualTo(LetterState.Created);
    }

    @Test
    public void compatible_with_existing_records() throws JsonProcessingException {
        // Save a letter using the existing repository code.
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        ObjectMapper objectMapper = new ObjectMapper();
        uk.gov.hmcts.reform.sendletter.data.LetterRepository repo =
            new uk.gov.hmcts.reform.sendletter.data.LetterRepository(jdbcTemplate, objectMapper);
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

    @Test
    public void generates_pdf() throws IOException {
        byte[] template = Resources.toByteArray(Resources.getResource("template.html"));
        Map<String, Object> content = ImmutableMap.of("name", "John");
        byte[] result = PdfCreator.generatePdf(template, content);
        assertThat(result).isNotEmpty();
    }
}
