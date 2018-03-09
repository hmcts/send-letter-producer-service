package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.exception.UnableToCreateLetterException;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.NotPrintedLetter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.sql.Timestamp.from;
import static java.util.Objects.nonNull;

@Repository
public class LetterRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    private final String resubmissionIntervalSqlCondition;

    public LetterRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        @Value("${letter-resubmission-interval-in-hours}") int resubmissionInterval
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.resubmissionIntervalSqlCondition = String.format(
            "+ %d * interval '1 hour' < CURRENT_TIMESTAMP", resubmissionInterval
        );
    }

    public UUID save(DbLetter letter, Instant creationTime, String messageId) throws JsonProcessingException {
        return jdbcTemplate.execute(
            "WITH new_letter AS ("
                + "  INSERT INTO letters AS l"
                + "  (id, message_id, service, type, created_at, sent_to_print_at, printed_at, additional_data)"
                + "  VALUES"
                + "  (:id, :messageId, :service, :type, :createdAt, :sentToPrintAt, :printedAt, :additionalData::JSON)"
                + "  ON CONFLICT (message_id) DO UPDATE"
                + "  SET submit_index = l.submit_index + 1, resubmitted_at = CURRENT_TIMESTAMP"
                + "  WHERE l.message_id = :messageId"
                + "    AND ("
                + "      (l.resubmitted_at IS NULL AND l.created_at " + resubmissionIntervalSqlCondition + ") OR"
                + "      (l.resubmitted_at IS NOT NULL AND l.resubmitted_at " + resubmissionIntervalSqlCondition + ")"
                + "    )"
                + "  RETURNING l.id"
                + ") "
                + "SELECT id FROM new_letter "
                + "UNION ALL "
                + "SELECT id FROM letters "
                + "WHERE message_id = :messageId "
                + "LIMIT 1",
            new MapSqlParameterSource()
                .addValue("id", letter.id)
                .addValue("messageId", messageId)
                .addValue("service", letter.service)
                .addValue("type", letter.type)
                .addValue("createdAt", from(creationTime))
                .addValue("additionalData", convertToJson(letter.additionalData))
                .addValue("sentToPrintAt", null)
                .addValue("printedAt", null),
            this::getLetterId
        );
    }

    private UUID getLetterId(PreparedStatement ps) throws SQLException {
        List<UUID> uuids = new LinkedList<>();

        try (ResultSet result = ps.executeQuery()) {
            while (result.next()) {
                uuids.add(result.getObject("id", UUID.class));
            }
        }

        return uuids.stream().findFirst().orElseThrow(UnableToCreateLetterException::new);
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
