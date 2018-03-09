package uk.gov.hmcts.reform.slc.services.steps.sftpupload;

import uk.gov.hmcts.reform.slc.model.LetterPrintStatus;

import java.util.List;

public class ParsedReport {

    public final String path;
    public final List<LetterPrintStatus> statuses;

    public ParsedReport(String path, List<LetterPrintStatus> statuses) {
        this.path = path;
        this.statuses = statuses;
    }
}
