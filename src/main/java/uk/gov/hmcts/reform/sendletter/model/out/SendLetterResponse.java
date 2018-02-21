package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

public class SendLetterResponse {

    @ApiModelProperty(
        name = "Letter Id",
        notes = "Id of the letter sent to print"
    )
    @JsonProperty("letter_id")
    public final UUID letterId;

    public SendLetterResponse(UUID letterId) {
        this.letterId = letterId;
    }
}
