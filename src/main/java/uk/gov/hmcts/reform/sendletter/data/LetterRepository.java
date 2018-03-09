package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.NotPrintedLetter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.sql.Timestamp.from;
import static java.util.Objects.nonNull;

@Repository
public class LetterRepository {

    private static final Logger log = LoggerFactory.getLogger(LetterRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public LetterRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(DbLetter letter, Instant creationTime, String messageId) throws JsonProcessingException {
        jdbcTemplate.update(
            "INSERT INTO letters "
                + "(id, message_id, service, type, created_at, sent_to_print_at, printed_at, additional_data, state) "
                + "VALUES "
                + "(:id, :messageId, :service, :type, :createdAt, :sentToPrintAt, :printedAt, :additionalData::JSON, "
                + ":state)",
            new MapSqlParameterSource()
                .addValue("id", letter.id)
                .addValue("messageId", messageId)
                .addValue("service", letter.service)
                .addValue("type", letter.type)
                .addValue("state", LetterState.Created.toString())
                .addValue("createdAt", from(creationTime))
                .addValue("additionalData", convertToJson(letter.additionalData))
                .addValue("sentToPrintAt", null)
                .addValue("printedAt", null)
        );
        log.info("Successfully saved letter data into database with id : {} and messageId :{}", letter.id, messageId);
    }

    /**
     * Retrieve letter status with given ID and service name.
     *
     * @param id          UUID
     * @param serviceName String
     * @return Letter status.
     */
    public Optional<LetterStatus> getLetterStatus(UUID id, String serviceName) {
        try {
            LetterStatus status = jdbcTemplate.queryForObject(
                "SELECT id, message_id, created_at, sent_to_print_at, printed_at, is_failed "
                    + "FROM letters "
                    + "WHERE id = :id AND service = :service",
                new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("service", serviceName),
                LetterMapperFactory.LETTER_STATUS_MAPPER
            );

            return Optional.of(status);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * Updates the `sent_to_print_at` column on letter(s) with given id.
     *
     * @return number of updated rows.
     */
    public int updateSentToPrintAt(UUID id, LocalDateTime dateTime) {
        return jdbcTemplate.update(
            "UPDATE letters SET sent_to_print_at = :sentToPrintAt WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("sentToPrintAt", dateTime)
        );
    }

    /**
     * Updates the `printed_at` column on letter(s) with given id.
     *
     * @return number of updated rows.
     */
    public int updatePrintedAt(UUID id, LocalDateTime dateTime) {
        return jdbcTemplate.update(
            "UPDATE letters SET printed_at = :printedAt WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("printedAt", dateTime)
        );
    }

    /**
     * Updates the `is_failed` column to `true`  on letter with given id.
     *
     * @return number of updated rows.
     */
    public int updateIsFailed(UUID id) {
        return jdbcTemplate.update(
            "UPDATE letters SET is_failed = :isFailed WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("isFailed", true)
        );
    }

    /**
     * Retrieve a list of letters which are not failed but still not printed.
     *
     * @return a list of unprinted letters which were sent before yesterdays 5pm deadline
     */
    public List<NotPrintedLetter> getStaleLetters() {
        return jdbcTemplate.query(
            "SELECT id, message_id, service, type, created_at, sent_to_print_at "
                + "FROM letters "
                + "WHERE NOT is_failed"
                + "  AND printed_at IS NULL"
                + "  AND sent_to_print_at IS NOT NULL"
                + "  AND sent_to_print_at < (CURRENT_DATE - integer '1' + time '17:00')",
            LetterMapperFactory.NOT_PRINTED_LETTER_MAPPER
        );
    }

    private String convertToJson(Map<String, Object> additionalData) throws JsonProcessingException {
        if (nonNull(additionalData)) {
            return objectMapper.writeValueAsString(additionalData);
        }
        return null;
    }
}
