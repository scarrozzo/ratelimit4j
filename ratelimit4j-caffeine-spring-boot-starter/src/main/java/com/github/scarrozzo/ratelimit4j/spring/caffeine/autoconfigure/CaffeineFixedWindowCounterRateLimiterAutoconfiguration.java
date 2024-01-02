package com.github.scarrozzo.ratelimit4j.spring.caffeine.autoconfigure;

import com.github.scarrozzo.ratelimit4j.caffeine.algorithm.CaffeineFixedWindowCounterRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.config.RateLimiterType;
import com.github.scarrozzo.ratelimit4j.spring.caffeine.config.CaffeineFixedWindowCounterRateLimiterProperties;
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
@ConditionalOnClass(CaffeineFixedWindowCounterRateLimiter.class)
@EnableConfigurationProperties({CaffeineFixedWindowCounterRateLimiterProperties.class, SpringBootRateLimiterProperties.class})
public class CaffeineFixedWindowCounterRateLimiterAutoconfiguration implements WebMvcConfigurer {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    @ConditionalOnMissingBean
    public FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig(CaffeineFixedWindowCounterRateLimiterProperties caffeineFixedWindowCounterRateLimiterProperties) {
        long numberOfRequests = CaffeineFixedWindowCounterRateLimiterProperties.DEFAULT_NUMBER_OF_REQUESTS;
        long windowSize = CaffeineFixedWindowCounterRateLimiterProperties.DEFAULT_WINDOW_SIZE_IN_MILLISECS;

        if (caffeineFixedWindowCounterRateLimiterProperties != null && caffeineFixedWindowCounterRateLimiterProperties.getNumberOfRequests() != null) {
            numberOfRequests = caffeineFixedWindowCounterRateLimiterProperties.getNumberOfRequests();
        }

        if (caffeineFixedWindowCounterRateLimiterProperties != null && caffeineFixedWindowCounterRateLimiterProperties.getWindowSize() != null) {
            windowSize = caffeineFixedWindowCounterRateLimiterProperties.getWindowSize();
        }

        return new FixedWindowCounterRateLimiterConfig(windowSize, numberOfRequests);
    }

    @Bean
    @ConditionalOnMissingBean
    public CaffeineFixedWindowCounterRateLimiter caffeineFixedWindowCounterRateLimiter(FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig) {
        return new CaffeineFixedWindowCounterRateLimiter(fixedWindowCounterRateLimiterConfig);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SpringBootRateLimiterProperties springBootRateLimiterProperties = beanFactory.getBean(SpringBootRateLimiterProperties.class);
        CaffeineFixedWindowCounterRateLimiter caffeineFixedWindowCounterRateLimiter = beanFactory.getBean(CaffeineFixedWindowCounterRateLimiter.class);
        if (springBootRateLimiterProperties.getLimiterTypes() != null &&
                springBootRateLimiterProperties.getLimiterTypes().contains(RateLimiterType.FIXED_WINDOW_COUNTER)) {
            registry.addInterceptor(new RateLimiterRequestInterceptor<>(springBootRateLimiterProperties, caffeineFixedWindowCounterRateLimiter));
        }
    }

}
