package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Size;

public class Letter implements Serializable {

    private static final long serialVersionUID = -7737087336283080072L;

    @ApiModelProperty(value = "List of documents to be printed. Maximum allowed is 10", required = true)
    @Size(min = 1, max = 10)
    @Valid
    public final List<Document> documents;

    @ApiModelProperty(value = "Type to be used by Xerox to print documents", required = true)
    @NotEmpty
    public final String type;

    @JsonProperty("additional_data")
    public final Map<String, Object> additionalData;

    public Letter(
        @JsonProperty("documents") List<Document> documents,
        @JsonProperty("type") String type,
        @JsonProperty("additional_data") Map<String, Object> additionalData
    ) {
        this.documents = documents;
        this.type = type;
        this.additionalData = additionalData;
    }
}
