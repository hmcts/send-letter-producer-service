package uk.gov.hmcts.reform.sendletter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class LetterSentToPrintAtPatch {

    public final LocalDateTime sentToPrintAt;

    public LetterSentToPrintAtPatch(
        @JsonProperty("sent_to_print_at") LocalDateTime sentToPrintAt
    ) {
        this.sentToPrintAt = sentToPrintAt;
    }
}
