package io.github.scarrozzo.ratelimit4j.spring.core.interceptor;

import io.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import io.github.scarrozzo.ratelimit4j.spring.core.config.ClientType;
import io.github.scarrozzo.ratelimit4j.spring.core.config.SpringBootRateLimiterProperties;
import io.github.scarrozzo.ratelimit4j.spring.core.evaluator.HttpRequestEvaluator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Slf4j
public class RateLimiterRequestInterceptor<T extends RateLimiter> implements HandlerInterceptor {

    private ClientType clientType;
    private List<String> analyzedPaths;
    private final T rateLimiter;

    public RateLimiterRequestInterceptor(SpringBootRateLimiterProperties springBootRateLimiterProperties, T rateLimiter) {
        log.trace("Spring boot properties: " + springBootRateLimiterProperties);
        if (springBootRateLimiterProperties.getClientType() != null) {
            this.clientType = springBootRateLimiterProperties.getClientType();
        }

        if (springBootRateLimiterProperties.getAnalyzedPaths() != null) {
            this.analyzedPaths = springBootRateLimiterProperties.getAnalyzedPaths();
        }

        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) {
        log.trace("preHandler called.");
        HttpRequestEvaluator.resolve(clientType, request, analyzedPaths, rateLimiter);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object object, ModelAndView model) {
        log.trace("postHandler called.");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object object, Exception exception) {
        log.trace("afterCompletion called.");
    }
}
