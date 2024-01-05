package io.github.scarrozzo.ratelimit4j.spring.redis.autoconfigure;

import io.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import io.github.scarrozzo.ratelimit4j.redis.algorithm.RedisFixedWindowCounterRateLimiter;
import io.github.scarrozzo.ratelimit4j.spring.core.config.SpringBootRateLimiterProperties;
import io.github.scarrozzo.ratelimit4j.spring.core.interceptor.RateLimiterRequestInterceptor;
import io.github.scarrozzo.ratelimit4j.spring.redis.config.RedisFixedWindowCounterRateLimiterProperties;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean({RedissonClient.class, TransactionOptions.class})
@ConditionalOnClass(RedisFixedWindowCounterRateLimiter.class)
@EnableConfigurationProperties({RedisFixedWindowCounterRateLimiterProperties.class, SpringBootRateLimiterProperties.class})
public class RedisFixedWindowCounterRateLimiterAutoconfiguration implements WebMvcConfigurer {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    public FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig(RedisFixedWindowCounterRateLimiterProperties redisFixedWindowCounterRateLimiterProperties) {
        long numberOfRequests = RedisFixedWindowCounterRateLimiterProperties.DEFAULT_NUMBER_OF_REQUESTS;
        long windowSize = RedisFixedWindowCounterRateLimiterProperties.DEFAULT_WINDOW_SIZE_IN_MILLISECS;

        if (redisFixedWindowCounterRateLimiterProperties != null && redisFixedWindowCounterRateLimiterProperties.getNumberOfRequests() != null) {
            numberOfRequests = redisFixedWindowCounterRateLimiterProperties.getNumberOfRequests();
        }

        if (redisFixedWindowCounterRateLimiterProperties != null && redisFixedWindowCounterRateLimiterProperties.getWindowSize() != null) {
            windowSize = redisFixedWindowCounterRateLimiterProperties.getWindowSize();
        }

        return new FixedWindowCounterRateLimiterConfig(windowSize, numberOfRequests);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisFixedWindowCounterRateLimiter redisFixedWindowCounterRateLimiter(FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig,
                                                                                 RedissonClient redissonClient,
                                                                                 TransactionOptions transactionOptions) {
        return new RedisFixedWindowCounterRateLimiter(fixedWindowCounterRateLimiterConfig, redissonClient, transactionOptions);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SpringBootRateLimiterProperties springBootRateLimiterProperties = beanFactory.getBean(SpringBootRateLimiterProperties.class);
        RedisFixedWindowCounterRateLimiter redisFixedWindowCounterRateLimiter = beanFactory.getBean(RedisFixedWindowCounterRateLimiter.class);
        if (springBootRateLimiterProperties.getLimiterTypes() != null &&
                springBootRateLimiterProperties.getLimiterTypes().contains(RateLimiterType.FIXED_WINDOW_COUNTER)) {
            registry.addInterceptor(new RateLimiterRequestInterceptor<>(springBootRateLimiterProperties, redisFixedWindowCounterRateLimiter));
        }
    }

}
