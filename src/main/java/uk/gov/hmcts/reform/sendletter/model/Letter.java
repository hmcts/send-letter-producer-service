package uk.gov.hmcts.reform.sendletter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.Map;

public class Letter implements Serializable {

    private static final long serialVersionUID = -7737087336283080072L;

    @NotEmpty
    public final String template;

    @NotEmpty
    public final Map<String, String> values;

    /** Used by Xerox. */
    @NotEmpty
    public final String type;

    public Letter(
        @JsonProperty("template") String template,
        @JsonProperty("values") Map<String, String> values,
        @JsonProperty("type") String type
    ) {
        this.template = template;
        this.values = values;
        this.type = type;
    }
}
