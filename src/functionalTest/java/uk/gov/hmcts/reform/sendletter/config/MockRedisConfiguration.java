package uk.gov.hmcts.reform.sendletter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
public class MockRedisConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MockRedisConfiguration.class);

    static class MockRedisServer {

        @Value("${redis.port}")
        private int port;

        private GenericContainer container;

        @PostConstruct
        public void init() {
            container = new FixedHostPortGenericContainer("redis:4")
                .withFixedExposedPort(port, port);
            container.start();

            System.setProperty("redis.host", container.getContainerIpAddress());
        }

        @PreDestroy
        public void destroy() {
            container.stop();
        }
    }

    @Bean("redisServer")
    public MockRedisServer getMockRedisServer(@Value("${redis.enabled}") boolean isRedisEnabled) {
        if (isRedisEnabled) {
            log.info("Using REDIS Test Container");

            return new MockRedisServer();
        } else {
            log.info("No need for Redis Server. Make sure your test replicates that");

            return null;
        }
    }
}
