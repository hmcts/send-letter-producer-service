package uk.gov.hmcts.reform.slc.logging;

/**
 * Used to track dependency of how long in millis did it take to perform a command.
 */
final class AppDependency {

    /**
     * Service bus message receiver dependency.
     */
    static final String SERVICE_BUS = "ServiceBus";

    /**
     * Pdf service client.
     */
    static final String PDF_GENERATOR = "PdfGenerator";

    /**
     * Ftp client.
     */
    static final String FTP_CLIENT = "FtpClient";

    private AppDependency() {
        // utility class constructor
    }
}
