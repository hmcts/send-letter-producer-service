package uk.gov.hmcts.reform.slc.services.steps.sftpupload;

public class Report {

    public final String path;
    public final byte[] content;

    public Report(String path, byte[] content) {
        this.path = path;
        this.content = content;
    }
}
