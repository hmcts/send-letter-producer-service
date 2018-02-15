package uk.gov.hmcts.reform.sendletter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class LetterPrintedAtPatch {

    public final LocalDateTime printedAt;

    public LetterPrintedAtPatch(
        @JsonProperty("printed_at") LocalDateTime printedAt
    ) {
        this.printedAt = printedAt;
    }
}
