package uk.gov.hmcts.reform.sendletter.model.out;

import java.time.ZonedDateTime;
import java.util.UUID;

public class NotPrintedLetter {

    public final UUID id;

    public final String messageId;

    public final String service;

    public final String type;

    public final ZonedDateTime createdAt;

    public final ZonedDateTime sentToPrintAt;

    public NotPrintedLetter(
        final UUID id,
        final String messageId,
        final String service,
        final String type,
        final ZonedDateTime createdAt,
        final ZonedDateTime sentToPrintAt
    ) {
        this.id = id;
        this.messageId = messageId;
        this.service = service;
        this.type = type;
        this.createdAt = createdAt;
        this.sentToPrintAt = sentToPrintAt;
    }
}
