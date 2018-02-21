package uk.gov.hmcts.reform.sendletter.data.model;

import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DbLetter {

    public final UUID id;
    public final List<Document> documents;
    public final String type;
    public final String service;
    public final Map<String, Object> additionalData;

    public DbLetter(
        UUID id,
        List<Document> documents,
        String type,
        String service,
        Map<String, Object> additionalData
    ) {
        this.id = id;
        this.documents = documents;
        this.type = type;
        this.service = service;
        this.additionalData = additionalData;
    }

    public DbLetter(UUID id, String service, Letter letter) {
        this(id, letter.documents, letter.type, service, letter.additionalData);
    }
}
