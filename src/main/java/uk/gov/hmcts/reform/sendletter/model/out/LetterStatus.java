package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.UUID;

public class LetterStatus {

    public final UUID id;

    @JsonProperty("message_id")
    public final String messageId;

    @JsonProperty("created_at")
    public final ZonedDateTime createdAt;

    @JsonProperty("sent_to_print_at")
    public final ZonedDateTime sentToPrintAt;

    @JsonProperty("printed_at")
    public final ZonedDateTime printedAt;

    @JsonProperty("has_failed")
    public final boolean hasFailed;

    public LetterStatus(
        final UUID id,
        final String messageId,
        final ZonedDateTime createdAt,
        final ZonedDateTime sentToPrintAt,
        final ZonedDateTime printedAt,
        final boolean hasFailed
    ) {
        this.id = id;
        this.messageId = messageId;
        this.createdAt = createdAt;
        this.sentToPrintAt = sentToPrintAt;
        this.printedAt = printedAt;
        this.hasFailed = hasFailed;
    }
}
