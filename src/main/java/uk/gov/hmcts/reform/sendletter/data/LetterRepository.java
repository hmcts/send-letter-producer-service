package uk.gov.hmcts.reform.sendletter.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.sendletter.model.Letter;
import uk.gov.hmcts.reform.sendletter.model.WithServiceNameAndId;

import java.time.Instant;
import java.util.Map;

import static java.sql.Timestamp.from;
import static java.util.Objects.nonNull;

@SuppressWarnings("checkstyle:LineLength")
@Repository
public class LetterRepository {

    private static final Logger log = LoggerFactory.getLogger(LetterRepository.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public LetterRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(WithServiceNameAndId<Letter> letterWithServiceNameAndId, Instant messageSendTime, String messageId) throws JsonProcessingException {
        jdbcTemplate.update(
            "INSERT INTO letters (id, message_id, service, created_at, sent_to_print_at, printed_at, additional_data)"
                + "VALUES (:id, :messageId, :service, :createdAt, :sentToPrintAt, :printedAt, :additionalData::JSON)",
            new MapSqlParameterSource()
                .addValue("id", letterWithServiceNameAndId.id)
                .addValue("messageId", messageId)
                .addValue("service", letterWithServiceNameAndId.service)
                .addValue("createdAt", from(messageSendTime))
                .addValue("additionalData", convertToJson(letterWithServiceNameAndId.obj.additionalData))
                .addValue("sentToPrintAt", null)
                .addValue("printedAt", null)
        );
        log.info("Successfully saved letter data into database with id : {} and messageId :{}", letterWithServiceNameAndId.id, messageId);
    }

    private String convertToJson(Map<String, Object> additionalData) throws JsonProcessingException {
        if (nonNull(additionalData)) {
            return objectMapper.writeValueAsString(additionalData);
        }
        return null;
    }
}
