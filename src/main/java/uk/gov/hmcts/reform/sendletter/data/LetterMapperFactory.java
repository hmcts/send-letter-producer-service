package uk.gov.hmcts.reform.sendletter.data;

import org.springframework.jdbc.core.RowMapper;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.NotPrintedLetter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings({"squid:S1118", "HideUtilityClassConstructor"})
final class LetterMapperFactory {

    public static final RowMapper<LetterStatus> LETTER_STATUS_MAPPER = new LetterStatusMapper();
    public static final RowMapper<NotPrintedLetter> NOT_PRINTED_LETTER_MAPPER = new NotPrintedMapper();

    static ZonedDateTime getDateTime(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnLabel);

        if (timestamp == null) {
            return null;
        }

        return timestamp.toInstant().atZone(ZoneOffset.UTC);
    }

    private static final class LetterStatusMapper implements RowMapper<LetterStatus> {

        @Override
        public LetterStatus mapRow(ResultSet rs, int rowNumber) throws SQLException {
            return new LetterStatus(
                rs.getObject("id", UUID.class),
                rs.getString("message_id"),
                getDateTime(rs, "created_at"),
                getDateTime(rs, "sent_to_print_at"),
                getDateTime(rs, "printed_at"),
                rs.getBoolean("is_failed")
            );
        }
    }

    private static final class NotPrintedMapper implements RowMapper<NotPrintedLetter> {

        @Override
        public NotPrintedLetter mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new NotPrintedLetter(
                rs.getObject("id", UUID.class),
                rs.getString("message_id"),
                rs.getString("service"),
                rs.getString("type"),
                getDateTime(rs, "created_at"),
                getDateTime(rs, "sent_to_print_at")
            );
        }
    }
}
