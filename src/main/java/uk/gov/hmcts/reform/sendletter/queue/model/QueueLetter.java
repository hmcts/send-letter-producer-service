package uk.gov.hmcts.reform.sendletter.queue.model;

import uk.gov.hmcts.reform.sendletter.model.in.Document;

import java.util.List;
import java.util.UUID;

public class QueueLetter {

    public final UUID id;
    public final List<Document> documents;
    public final String type;
    public final String service;

    // region constructor
    public QueueLetter(
        UUID id,
        List<Document> documents,
        String type,
        String service
    ) {
        this.id = id;
        this.documents = documents;
        this.type = type;
        this.service = service;
    }
    // endregion
}
