package com.urlshortener.UrlShortenerService.service;

import com.urlshortener.UrlShortenerService.client.CounterServerClient;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.ShortenRequest;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.ShortenResponse;
import com.urlshortener.UrlShortenerService.model.Url;
import com.urlshortener.UrlShortenerService.repository.UrlRepository;
import com.urlshortener.UrlShortenerService.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final Base62Encoder encoder;
    private final CounterServerClient counterClient;
    private final UrlRepository urlRepository;
    private final StringRedisTemplate redis;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.url-default-ttl-days}")
    private int defaultTtlDays;

    private static final String REDIS_KEY_PREFIX = "url:";

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String shortCode = resolveShortCode(request);
        int ttlDays = request.getTtlDays() != null
                ? request.getTtlDays()
                : defaultTtlDays;
        Instant expiresAt = Instant.now().plus(Duration.ofDays(ttlDays));

        // 1. Save to PostgreSQL
        Url url = Url.builder()
                .shortCode(shortCode)
                .longUrl(request.getLongUrl())
                .expiresAt(expiresAt)
                .build();
        urlRepository.save(url);

        // 2. Write to Redis — for fast redirect lookups
        redis.opsForValue().set(
                REDIS_KEY_PREFIX + shortCode,
                request.getLongUrl(),
                Duration.ofDays(ttlDays)
        );

        log.info("Shortened '{}' → '{}'", request.getLongUrl(), shortCode);

        return ShortenResponse.builder()
                .shortUrl(baseUrl + "/" + shortCode)
                .shortCode(shortCode)
                .longUrl(request.getLongUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(expiresAt)
                .build();
    }

    private String resolveShortCode(ShortenRequest request) {
        // Custom code requested — validate it's not already taken
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            String custom = request.getCustomCode().trim();
            if (urlRepository.existsByShortCode(custom)) {
                throw new ShortCodeConflictException(
                        "Custom code '" + custom + "' is already taken");
            }
            return custom;
        }

        // Generate code from counter server sequence
        long sequence = counterClient.nextSequence();
        return encoder.encode(sequence);
    }

    public Optional<String> resolve(String shortCode) {

        // Step 1 — Redis
        String cached = redis.opsForValue().get(REDIS_KEY_PREFIX + shortCode);
        if (cached != null) {
            log.debug("Redis hit for '{}'", shortCode);
            return Optional.of(cached);
        }

        // Step 2 — PostgreSQL
        log.debug("Redis miss for '{}' — querying PostgreSQL", shortCode);
        return urlRepository.findActiveByShortCode(shortCode)
                .map(url -> {
                    // Step 3 — fill up cache
                    Duration remainingTtl = url.getExpiresAt() != null
                            ? Duration.between(Instant.now(), url.getExpiresAt())
                            : Duration.ofDays(defaultTtlDays);

                    if (!remainingTtl.isNegative()) {
                        redis.opsForValue().set(
                                REDIS_KEY_PREFIX + shortCode,
                                url.getLongUrl(),
                                remainingTtl
                        );
                    }
                    return url.getLongUrl();
                });
    }

    @Transactional
    public boolean deactivate(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .map(url -> {
                    url.setActive(false);
                    urlRepository.save(url);
                    redis.delete(REDIS_KEY_PREFIX + shortCode);
                    log.info("Deactivated '{}'", shortCode);
                    return true;
                })
                .orElse(false);
    }


    public static class ShortCodeConflictException extends RuntimeException {
        public ShortCodeConflictException(String msg) {
            super(msg);
        }
    }
}
