package com.github.scarrozzo.ratelimit4j.spring.redis.autoconfigure;

import com.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import com.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.redis.algorithm.RedisTokenBucketRateLimiter;
import com.github.scarrozzo.ratelimit4j.spring.core.config.SpringBootRateLimiterProperties;
import com.github.scarrozzo.ratelimit4j.spring.core.interceptor.RateLimiterRequestInterceptor;
import com.github.scarrozzo.ratelimit4j.spring.redis.config.RedisTokenBucketRateLimiterProperties;
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
@ConditionalOnClass(RedisTokenBucketRateLimiter.class)
@EnableConfigurationProperties({RedisTokenBucketRateLimiterProperties.class, SpringBootRateLimiterProperties.class})
public class RedisTokenBucketRateLimiterAutoconfiguration implements WebMvcConfigurer {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    public TokenBucketRateLimiterConfig bucketRateLimiterConfig(RedisTokenBucketRateLimiterProperties redisTokenBucketRateLimiterProperties) {
        long bucketSize = RedisTokenBucketRateLimiterProperties.DEFAULT_BUCKET_SIZE;
        long refillPeriodInMilliSeconds = RedisTokenBucketRateLimiterProperties.DEFAULT_REFILL_PERIOD_IN_MILLISECS;

        if (redisTokenBucketRateLimiterProperties != null && redisTokenBucketRateLimiterProperties.getBucketSize() != null) {
            bucketSize = redisTokenBucketRateLimiterProperties.getBucketSize();
        }

        if (redisTokenBucketRateLimiterProperties != null && redisTokenBucketRateLimiterProperties.getRefillPeriodInMilliSeconds() != null) {
            refillPeriodInMilliSeconds = redisTokenBucketRateLimiterProperties.getRefillPeriodInMilliSeconds();
        }

        return new TokenBucketRateLimiterConfig(bucketSize, refillPeriodInMilliSeconds);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTokenBucketRateLimiter redisTokenBucketRateLimiter(TokenBucketRateLimiterConfig tokenBucketRateLimiterConfig,
                                                                   RedissonClient redissonClient,
                                                                   TransactionOptions transactionOptions) {
        return new RedisTokenBucketRateLimiter(tokenBucketRateLimiterConfig, redissonClient, transactionOptions);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SpringBootRateLimiterProperties springBootRateLimiterProperties = beanFactory.getBean(SpringBootRateLimiterProperties.class);
        RedisTokenBucketRateLimiter redisTokenBucketRateLimiter = beanFactory.getBean(RedisTokenBucketRateLimiter.class);
        if (springBootRateLimiterProperties.getLimiterTypes() != null &&
                springBootRateLimiterProperties.getLimiterTypes().contains(RateLimiterType.TOKEN_BUCKET)) {
            registry.addInterceptor(new RateLimiterRequestInterceptor<>(springBootRateLimiterProperties, redisTokenBucketRateLimiter));
        }
    }

}
