package com.urlshortener.UrlShortenerService.controller;

import com.urlshortener.UrlShortenerService.dto.UrlDtos.ShortenRequest;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.ShortenResponse;
import com.urlshortener.UrlShortenerService.service.AnalyticsService;
import com.urlshortener.UrlShortenerService.service.RateLimiter;
import com.urlshortener.UrlShortenerService.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService shortenerService;
    private final AnalyticsService analyticsService;
    private final RateLimiter rateLimiter;

    @PostMapping("/shorten")
    public ResponseEntity<?> shorten(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {

        String ip = extractIp(httpRequest);

        if (!rateLimiter.allowShorten(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody(429, "Too Many Requests",
                            "Shorten rate limit exceeded. Max " +
                                    "requests per minute reached."));
        }

        ShortenResponse response = shortenerService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(
            @PathVariable String shortCode,
            HttpServletRequest httpRequest) {

        String ip = extractIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String referer = httpRequest.getHeader("Referer");

        if (!rateLimiter.allowRedirect(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(errorBody(429, "Too Many Requests",
                            "Redirect rate limit exceeded."));
        }

        return shortenerService.resolve(shortCode)
                .<ResponseEntity<?>>map(longUrl -> {
                    // All values extracted above on main thread — safe to pass to async
                    analyticsService.recordClick(shortCode, ip, userAgent, referer);

                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(longUrl))
                            .build();
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorBody(404, "Not Found",
                                "Short code '" + shortCode + "' does not exist or has expired")));
    }

    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<?> analytics(@PathVariable String shortCode) {
        return analyticsService.getAnalytics(shortCode)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorBody(404, "Not Found",
                                "Short code '" + shortCode + "' does not exist")));
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<?> deactivate(@PathVariable String shortCode) {
        boolean deactivated = shortenerService.deactivate(shortCode);
        if (!deactivated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody(404, "Not Found",
                            "Short code '" + shortCode + "' does not exist"));
        }
        return ResponseEntity.noContent().build();
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        return Map.of(
                "status", status,
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString()
        );
    }
}
