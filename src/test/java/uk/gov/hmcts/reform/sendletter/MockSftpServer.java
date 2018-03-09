package uk.gov.hmcts.reform.sendletter;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemView;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.subsystem.SftpSubsystem;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;

public class MockSftpServer implements AutoCloseable {
    private SshServer sshd;

    public static final int port = 8001;

    public MockSftpServer(TemporaryFolder testFolder) throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setFileSystemFactory(new NativeFileSystemFactory() {
            @Override
            public FileSystemView createFileSystemView(final Session session) {
                return new NativeFileSystemView(session.getUsername(), false) {
                    @Override
                    public String getVirtualUserDir() {
                        return testFolder.getRoot().getAbsolutePath();
                    }
                };
            }
        });

        sshd.setPort(port);
        sshd.setSubsystemFactories(Arrays.asList(new SftpSubsystem.Factory()));
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        // Disable SSL and password checks.
        sshd.setPasswordAuthenticator((a, b, c) -> true);
        sshd.setPublickeyAuthenticator((a, b, c) -> true);

        sshd.start();
    }

    @Override
    public void close() throws Exception {
        sshd.stop();
    }
}
