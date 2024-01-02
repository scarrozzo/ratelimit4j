package com.github.scarrozzo.ratelimit4j.spring.core.evaluator;


import com.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.RateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.spring.core.config.ClientType;
import com.github.scarrozzo.ratelimit4j.spring.core.exception.InvalidClientTypeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class HttpRequestEvaluator {

    private static final String[] IP_FORWARD_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"};

    private HttpRequestEvaluator() {
    }

    public static <T extends RateLimiterConfig> void resolve(ClientType clientType,
                                                             HttpServletRequest request,
                                                             List<String> analyzedPaths,
                                                             RateLimiter<T> rateLimiter) {
        switch (clientType) {
            case IP_ADDRESS -> evaluateByIpAddress(request, analyzedPaths, rateLimiter);
            case JWT -> evaluateByJwt(request, analyzedPaths, rateLimiter);
            default -> throw new InvalidClientTypeException();
        }
    }

    private static <T extends RateLimiterConfig> void evaluateByIpAddress(HttpServletRequest request,
                                                                          List<String> analyzedPaths,
                                                                          RateLimiter<T> rateLimiter) {
        if (request.getRequestURI() != null &&
                analyzedPaths.stream().anyMatch(path -> Pattern.matches(path, request.getRequestURI()))) {
            String ipAddress = getClientIpAddress(request);

            if (StringUtils.hasText(ipAddress)) {
                log.debug("Evaluating request for [ip={}, path={}, limiterType={}]", ipAddress, request.getRequestURI(),
                        rateLimiter.getRateLimiterConfig().getRateLimiterType());
                rateLimiter.evaluateRequest(ipAddress);
            } else {
                log.warn("Cannot evaluate request for [ip={}, path={}, limiterType={}] because ip address is empty",
                        ipAddress, request.getRequestURI(), rateLimiter.getRateLimiterConfig().getRateLimiterType());
            }
        } else {
            log.debug("Evaluate request not called for [path={}] because is not in the analyzed paths", request.getRequestURI());
        }
    }

    private static <T extends RateLimiterConfig> void evaluateByJwt(HttpServletRequest request,
                                                                    List<String> analyzedPaths,
                                                                    RateLimiter<T> rateLimiter) {
        if (request.getRequestURI() != null &&
                analyzedPaths.stream().anyMatch(path -> Pattern.matches(path, request.getRequestURI()))) {
            String jwt = getJwt(request);
            if (StringUtils.hasText(jwt)) {
                log.debug("Evaluating request for [jwt={}, path={}, limiterType={}]", jwt, request.getRequestURI(),
                        rateLimiter.getRateLimiterConfig().getRateLimiterType());
                rateLimiter.evaluateRequest(jwt);
            } else {
                log.warn("Cannot evaluate request for [jwt={}, path={}, limiterType={}] because jwt is empty", jwt,
                        request.getRequestURI(), rateLimiter.getRateLimiterConfig().getRateLimiterType());
            }
        } else {
            log.debug("Evaluate request not called for [path={}] because is not in the analyzed paths", request.getRequestURI());
        }
    }

    private static String getClientIpAddress(HttpServletRequest request) {
        for (String header : IP_FORWARD_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private static String getJwt(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorizationHeader.replace("Bearer ", "");
    }
}
