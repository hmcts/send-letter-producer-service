package uk.gov.hmcts.reform.sendletter.config;

import net.schmizz.sshj.SSHClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class SftpConfig {

    @Bean
    public Supplier<SSHClient> sshClient() {
        return SSHClient::new;
    }
}
