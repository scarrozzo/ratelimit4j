package com.github.scarrozzo.ratelimit4j.spring.caffeine.autoconfigure;

import com.github.scarrozzo.ratelimit4j.caffeine.algorithm.CaffeineLeakyBucketRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import com.github.scarrozzo.ratelimit4j.spring.caffeine.config.CaffeineLeakyBucketRateLimiterProperties;
import com.github.scarrozzo.ratelimit4j.spring.core.config.SpringBootRateLimiterProperties;
import com.github.scarrozzo.ratelimit4j.spring.core.interceptor.RateLimiterRequestInterceptor;
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
@ConditionalOnClass(CaffeineLeakyBucketRateLimiter.class)
@EnableConfigurationProperties({CaffeineLeakyBucketRateLimiterProperties.class, SpringBootRateLimiterProperties.class})
public class CaffeineLeakyBucketRateLimiterAutoconfiguration implements WebMvcConfigurer {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    public LeakyBucketRateLimiterConfig leakyBucketRateLimiterConfig(CaffeineLeakyBucketRateLimiterProperties caffeineLeakyBucketRateLimiterProperties) {
        long bucketSize = CaffeineLeakyBucketRateLimiterProperties.DEFAULT_BUCKET_SIZE;
        long outflowRateNumReq = CaffeineLeakyBucketRateLimiterProperties.DEFAULT_OUTFLOW_RATE_NUM_REQ;
        long outflowRatePeriodInMillisecs = CaffeineLeakyBucketRateLimiterProperties.DEFAULT_OUTFLOW_RATE_PERIOD_IN_MILLISECS;
        long clearQueueAfterInactivityInMillisecs = CaffeineLeakyBucketRateLimiterProperties.DEFAULT_CLEAR_QUEUE_AFTER_INACTIVITY_IN_MILLISECS;

        if (caffeineLeakyBucketRateLimiterProperties != null && caffeineLeakyBucketRateLimiterProperties.getBucketSize() != null) {
            bucketSize = caffeineLeakyBucketRateLimiterProperties.getBucketSize();
        }

        if (caffeineLeakyBucketRateLimiterProperties != null && caffeineLeakyBucketRateLimiterProperties.getOutflowRateNumReq() != null) {
            outflowRateNumReq = caffeineLeakyBucketRateLimiterProperties.getOutflowRateNumReq();
        }

        if (caffeineLeakyBucketRateLimiterProperties != null && caffeineLeakyBucketRateLimiterProperties.getOutflowRatePeriodInMilliseconds() != null) {
            outflowRatePeriodInMillisecs = caffeineLeakyBucketRateLimiterProperties.getOutflowRatePeriodInMilliseconds();
        }

        if (caffeineLeakyBucketRateLimiterProperties != null && caffeineLeakyBucketRateLimiterProperties.getClearQueueAfterInactivityInMilliseconds() != null) {
            clearQueueAfterInactivityInMillisecs = caffeineLeakyBucketRateLimiterProperties.getClearQueueAfterInactivityInMilliseconds();
        }

        return new LeakyBucketRateLimiterConfig(bucketSize, outflowRateNumReq, outflowRatePeriodInMillisecs, clearQueueAfterInactivityInMillisecs);
    }

    @Bean
    @ConditionalOnMissingBean
    public CaffeineLeakyBucketRateLimiter caffeineLeakyBucketRateLimiter(LeakyBucketRateLimiterConfig leakyBucketRateLimiterConfig) {
        return new CaffeineLeakyBucketRateLimiter(leakyBucketRateLimiterConfig);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SpringBootRateLimiterProperties springBootRateLimiterProperties = beanFactory.getBean(SpringBootRateLimiterProperties.class);
        CaffeineLeakyBucketRateLimiter caffeineLeakyBucketRateLimiter = beanFactory.getBean(CaffeineLeakyBucketRateLimiter.class);
        if (springBootRateLimiterProperties.getLimiterTypes() != null &&
                springBootRateLimiterProperties.getLimiterTypes().contains(RateLimiterType.LEAKY_BUCKET)) {
            registry.addInterceptor(new RateLimiterRequestInterceptor<>(springBootRateLimiterProperties, caffeineLeakyBucketRateLimiter));
        }
    }

}
