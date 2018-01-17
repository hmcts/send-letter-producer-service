package uk.gov.hmcts.reform.sendletter.health;

import com.google.common.base.Strings;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;


@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisClientConfig config;

    public RedisHealthIndicator(
        @Value("${redis.host}") String host,
        @Value("${redis.port}") int port,
        @Value("${redis.password}") String password
    ) {
        RedisClientConfig config = new RedisClientConfig();
        config.setAddress(host, port);
        config.setPassword(Strings.isNullOrEmpty(password) ? null : password);

        this.config = config;
    }

    @Override
    public Health health() {
        try {
            RedisClient.create(config).connect();
            return Health.up().build();
        } catch (RedisConnectionException exc) {
            return Health.down().withException(exc).build();
        }
    }
}
