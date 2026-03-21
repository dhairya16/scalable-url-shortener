package com.urlshortener.UrlRedirectService.controller;

import com.urlshortener.UrlRedirectService.dto.ClickEventMessage;
import com.urlshortener.UrlRedirectService.service.ClickEventProducer;
import com.urlshortener.UrlRedirectService.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final StringRedisTemplate redis;
    private final ClickEventProducer producer;
    private final RateLimiter rateLimiter;

    private static final String REDIS_KEY_PREFIX = "url:";

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        // Ignore browser automatic requests
        if (shortCode.equals("favicon.ico") || shortCode.equals("robots.txt")) {
            return ResponseEntity.notFound().build();
        }

        String ip = extractIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        if (!rateLimiter.allowRedirect(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded"));
        }

        // Look up in Redis — this is the only data store this service touches
        String longUrl = redis.opsForValue().get(REDIS_KEY_PREFIX + shortCode);

        if (longUrl == null) {
            log.debug("Short code '{}' not found in Redis", shortCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Short code '" + shortCode + "' not found"));
        }

        // Publish click event to Kafka — non-blocking
        producer.publish(ClickEventMessage.builder()
                .shortCode(shortCode)
                .longUrl(longUrl)
                .clickedAt(Instant.now())
                .ipAddress(ip)
                .userAgent(userAgent != null ? userAgent : "")
                .referer(referer != null ? referer : "")
                .build());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}