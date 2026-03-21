package com.urlshortener.UrlRedirectService.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimiter {

    @Value("${rate-limit.redirect-rpm}")
    private int redirectRpm;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRedirect(String ip) {
        Bucket bucket = buckets.computeIfAbsent(
                "redirect:" + ip,
                key -> newBucket(redirectRpm)
        );
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) log.warn("Redirect rate limit exceeded for ip={}", ip);
        return allowed;
    }

    private Bucket newBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}