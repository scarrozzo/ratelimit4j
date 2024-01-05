package io.github.scarrozzo.ratelimit4j.spring.core.config;

import io.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "ratelimit4j.spring.web")
public class SpringBootRateLimiterProperties {
    protected static final ClientType DEFAULT_CLIENT_TYPE = ClientType.IP_ADDRESS;
    protected static final List<String> DEFAULT_ANALYZED_PATHS = new ArrayList<>();
    protected static final List<RateLimiterType> DEFAULT_RATE_LIMITER_TYPES = List.of(RateLimiterType.TOKEN_BUCKET);

    @NotNull
    private List<RateLimiterType> limiterTypes = DEFAULT_RATE_LIMITER_TYPES;
    private ClientType clientType = SpringBootRateLimiterProperties.DEFAULT_CLIENT_TYPE;
    private List<String> analyzedPaths = SpringBootRateLimiterProperties.DEFAULT_ANALYZED_PATHS;
}
