package io.github.scarrozzo.ratelimit4j.spring.caffeine.autoconfigure;

import io.github.scarrozzo.ratelimit4j.caffeine.algorithm.CaffeineTokenBucketRateLimiter;
import io.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import io.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.spring.caffeine.config.CaffeineTokenBucketRateLimiterProperties;
import io.github.scarrozzo.ratelimit4j.spring.core.config.SpringBootRateLimiterProperties;
import io.github.scarrozzo.ratelimit4j.spring.core.interceptor.RateLimiterRequestInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnClass(CaffeineTokenBucketRateLimiter.class)
@EnableConfigurationProperties({CaffeineTokenBucketRateLimiterProperties.class, SpringBootRateLimiterProperties.class})
public class CaffeineTokenBucketRateLimiterAutoconfiguration implements WebMvcConfigurer {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    public TokenBucketRateLimiterConfig bucketRateLimiterConfig(CaffeineTokenBucketRateLimiterProperties caffeineTokenBucketRateLimiterProperties) {
        long bucketSize = CaffeineTokenBucketRateLimiterProperties.DEFAULT_BUCKET_SIZE;
        long refillPeriodInMilliSeconds = CaffeineTokenBucketRateLimiterProperties.DEFAULT_REFILL_PERIOD_IN_MILLISECS;

        if (caffeineTokenBucketRateLimiterProperties != null && caffeineTokenBucketRateLimiterProperties.getBucketSize() != null) {
            bucketSize = caffeineTokenBucketRateLimiterProperties.getBucketSize();
        }

        if (caffeineTokenBucketRateLimiterProperties != null && caffeineTokenBucketRateLimiterProperties.getRefillPeriodInMilliSeconds() != null) {
            refillPeriodInMilliSeconds = caffeineTokenBucketRateLimiterProperties.getRefillPeriodInMilliSeconds();
        }

        return new TokenBucketRateLimiterConfig(bucketSize, refillPeriodInMilliSeconds);
    }

    @Bean
    @ConditionalOnMissingBean
    public CaffeineTokenBucketRateLimiter caffeineTokenBucketRateLimiter(TokenBucketRateLimiterConfig tokenBucketRateLimiterConfig) {
        return new CaffeineTokenBucketRateLimiter(tokenBucketRateLimiterConfig);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SpringBootRateLimiterProperties springBootRateLimiterProperties = beanFactory.getBean(SpringBootRateLimiterProperties.class);
        CaffeineTokenBucketRateLimiter caffeineTokenBucketRateLimiter = beanFactory.getBean(CaffeineTokenBucketRateLimiter.class);
        if (springBootRateLimiterProperties.getLimiterTypes() != null &&
                springBootRateLimiterProperties.getLimiterTypes().contains(RateLimiterType.TOKEN_BUCKET)) {
            registry.addInterceptor(new RateLimiterRequestInterceptor<>(springBootRateLimiterProperties, caffeineTokenBucketRateLimiter));
        }
    }

}
