package uk.gov.hmcts.reform.sendletter.data;

import org.springframework.jdbc.core.RowMapper;
import uk.gov.hmcts.reform.sendletter.domain.LetterStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

final class LetterMapperFactory {

    public static final RowMapper<LetterStatus> LETTER_STATUS_MAPPER = new LetterStatusMapper();

    private static final class LetterStatusMapper implements RowMapper<LetterStatus> {

        private ZonedDateTime getDateTime(ResultSet rs, String columnLabel) throws SQLException {
            return rs.getTimestamp(columnLabel).toInstant().atOffset(ZoneOffset.UTC).toZonedDateTime();
        }

        @Override
        public LetterStatus mapRow(ResultSet rs, int rowNumber) throws SQLException {
            return new LetterStatus(
                rs.getObject("id", UUID.class),
                rs.getString("message_id"),
                getDateTime(rs, "created_at"),
                getDateTime(rs, "sent_to_print_at"),
                getDateTime(rs, "printed_at")
            );
        }
    }

    private LetterMapperFactory() {
        // utility class constructor
    }
}
