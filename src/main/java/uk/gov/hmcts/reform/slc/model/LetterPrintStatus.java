package uk.gov.hmcts.reform.slc.model;

import java.time.ZonedDateTime;

public class LetterPrintStatus {

    public final String id;
    public final ZonedDateTime printedAt;

    public LetterPrintStatus(String id, ZonedDateTime printedAt) {
        this.id = id;
        this.printedAt = printedAt;
    }
}
