package uk.gov.hmcts.reform.sendletter.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.postgresql.util.PGobject;
import uk.gov.hmcts.reform.sendletter.model.in.Document;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="letters")
public class Letter {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private UUID id;

    public final String messageId;
    public final String service;
    public final String additionalData;
    public final Timestamp createdAt = Timestamp.from(Instant.now());
    public final String type;
    @Enumerated(EnumType.STRING)
    public final LetterState state = LetterState.Created;

    protected Letter() {
        messageId = null;
        service = null;
        additionalData = null;
        type = null;
    }

    public Letter(
        String messageId,
        String service,
        String additionalData,
        String type
    ) {
        this.messageId = messageId;
        this.service = service;
        this.additionalData = additionalData;
        this.type = type;
    }
}
