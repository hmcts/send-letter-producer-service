package uk.gov.hmcts.reform.slc.logging;

final class AppDependencyCommand {

    /**
     * Message received from service bus queue.
     */
    static final String SERVICE_BUS_MESSAGE_RECEIVED = "MessageReceived";

    /**
     * Lock released and message removed from service bus queue.
     */
    static final String SERVICE_BUS_MESSAGE_COMPLETED = "MessageCompleted";

    /**
     * Lock released and message send to dead letter pool.
     */
    static final String SERVICE_BUS_DEAD_LETTERED = "MessageDeadLettered";

    /**
     * PDF generated.
     */
    static final String GENERATED_PDF_FROM_HTML = "GeneratedPdfFromHtml";

    /**
     * File uploaded to ftp.
     */
    static final String FTP_FILE_UPLOADED = "FtpFileUploaded";

    private AppDependencyCommand() {
        // utility class constructor
    }
}
