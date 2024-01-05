package io.github.scarrozzo.ratelimit4j.spring.redis.autoconfigure;

import io.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import io.github.scarrozzo.ratelimit4j.redis.algorithm.RedisLeakyBucketRateLimiter;
import io.github.scarrozzo.ratelimit4j.spring.core.config.SpringBootRateLimiterProperties;
import io.github.scarrozzo.ratelimit4j.spring.core.interceptor.RateLimiterRequestInterceptor;
import io.github.scarrozzo.ratelimit4j.spring.redis.config.RedisLeakyBucketRateLimiterProperties;
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
@ConditionalOnClass(RedisLeakyBucketRateLimiter.class)
@EnableConfigurationProperties({RedisLeakyBucketRateLimiterProperties.class, SpringBootRateLimiterProperties.class})
public class RedisLeakyBucketRateLimiterAutoconfiguration implements WebMvcConfigurer {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    public LeakyBucketRateLimiterConfig leakyBucketRateLimiterConfig(RedisLeakyBucketRateLimiterProperties redisLeakyBucketRateLimiterProperties) {
        long bucketSize = RedisLeakyBucketRateLimiterProperties.DEFAULT_BUCKET_SIZE;
        long outflowRateNumReq = RedisLeakyBucketRateLimiterProperties.DEFAULT_OUTFLOW_RATE_NUM_REQ;
        long outflowRatePeriodInMillisecs = RedisLeakyBucketRateLimiterProperties.DEFAULT_OUTFLOW_RATE_PERIOD_IN_MILLISECS;
        long clearQueueAfterInactivityInMillisecs = RedisLeakyBucketRateLimiterProperties.DEFAULT_CLEAR_QUEUE_AFTER_INACTIVITY_IN_MILLISECS;

        if (redisLeakyBucketRateLimiterProperties != null && redisLeakyBucketRateLimiterProperties.getBucketSize() != null) {
            bucketSize = redisLeakyBucketRateLimiterProperties.getBucketSize();
        }

        if (redisLeakyBucketRateLimiterProperties != null && redisLeakyBucketRateLimiterProperties.getOutflowRateNumReq() != null) {
            outflowRateNumReq = redisLeakyBucketRateLimiterProperties.getOutflowRateNumReq();
        }

        if (redisLeakyBucketRateLimiterProperties != null && redisLeakyBucketRateLimiterProperties.getOutflowRatePeriodInMilliseconds() != null) {
            outflowRatePeriodInMillisecs = redisLeakyBucketRateLimiterProperties.getOutflowRatePeriodInMilliseconds();
        }

        if (redisLeakyBucketRateLimiterProperties != null && redisLeakyBucketRateLimiterProperties.getClearQueueAfterInactivityInMilliseconds() != null) {
            clearQueueAfterInactivityInMillisecs = redisLeakyBucketRateLimiterProperties.getClearQueueAfterInactivityInMilliseconds();
        }

        return new LeakyBucketRateLimiterConfig(bucketSize, outflowRateNumReq, outflowRatePeriodInMillisecs, clearQueueAfterInactivityInMillisecs);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisLeakyBucketRateLimiter redisLeakyBucketRateLimiter(LeakyBucketRateLimiterConfig leakyBucketRateLimiterConfig,
                                                                   RedissonClient redissonClient,
                                                                   TransactionOptions transactionOptions) {
        return new RedisLeakyBucketRateLimiter(leakyBucketRateLimiterConfig, redissonClient, transactionOptions);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SpringBootRateLimiterProperties springBootRateLimiterProperties = beanFactory.getBean(SpringBootRateLimiterProperties.class);
        RedisLeakyBucketRateLimiter redisLeakyBucketRateLimiter = beanFactory.getBean(RedisLeakyBucketRateLimiter.class);
        if (springBootRateLimiterProperties.getLimiterTypes() != null &&
                springBootRateLimiterProperties.getLimiterTypes().contains(RateLimiterType.LEAKY_BUCKET)) {
            registry.addInterceptor(new RateLimiterRequestInterceptor<>(springBootRateLimiterProperties, redisLeakyBucketRateLimiter));
        }
    }

}
