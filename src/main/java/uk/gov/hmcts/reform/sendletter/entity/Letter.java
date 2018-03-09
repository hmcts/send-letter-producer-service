package uk.gov.hmcts.reform.sendletter.entity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "letters")
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    public final String messageId;
    public final String service;
    public final String additionalData;
    public final Timestamp createdAt = Timestamp.from(Instant.now());
    public final String type;
    @Enumerated(EnumType.STRING)
    public final LetterState state = LetterState.Created;
    // Base64 encoded PDF.
    public final byte[] pdf;

    protected Letter() {
        messageId = null;
        service = null;
        additionalData = null;
        type = null;
        pdf = null;
    }

    public Letter(
        String messageId,
        String service,
        String additionalData,
        String type,
        byte[] pdf
    ) {
        this.messageId = messageId;
        this.service = service;
        this.additionalData = additionalData;
        this.type = type;
        this.pdf = pdf;
    }

}
