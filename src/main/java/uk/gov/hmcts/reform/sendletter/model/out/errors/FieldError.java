package uk.gov.hmcts.reform.sendletter.model.out.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldError {

    @JsonProperty("field_name")
    public final String fieldName;

    @JsonProperty("message")
    public final String message;

    public FieldError(String fieldName, String message) {
        this.fieldName = fieldName;
        this.message = message;
    }
}
