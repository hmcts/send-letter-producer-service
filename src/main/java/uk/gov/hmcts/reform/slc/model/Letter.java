package uk.gov.hmcts.reform.slc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;

public class Letter {

    @NotNull
    public final UUID id;

    @NotEmpty
    public final List<Document> documents;

    @NotEmpty
    public final String type;

    @NotEmpty
    public final String service;

    public Letter(
        @JsonProperty("id") UUID id,
        @JsonProperty("documents") List<Document> documents,
        @JsonProperty("type") String type,
        @JsonProperty("service") String service
    ) {
        this.id = id;
        this.documents = documents;
        this.type = type;
        this.service = service;
    }
}
