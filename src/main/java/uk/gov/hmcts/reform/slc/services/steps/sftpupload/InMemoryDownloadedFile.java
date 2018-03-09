package uk.gov.hmcts.reform.slc.services.steps.sftpupload;

import net.schmizz.sshj.xfer.InMemoryDestFile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class InMemoryDownloadedFile extends InMemoryDestFile {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}
