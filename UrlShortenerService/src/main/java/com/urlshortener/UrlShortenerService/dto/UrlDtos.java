package com.urlshortener.UrlShortenerService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;
import java.util.List;

public class UrlDtos {

    // Shorten request

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShortenRequest {

        @NotBlank(message = "longUrl must not be blank")
        @URL(message = "longUrl must be a valid URL")
        private String longUrl;

        // Optional — user can request a custom code like "my-sale"
        // 3 to 20 characters, only letters, numbers, and hyphens
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]{3,20}$",
                message = "customCode must be 3-20 alphanumeric characters or hyphens"
        )
        private String customCode;

        // Optional — defaults to app.url-default-ttl-days if not provided
        private Integer ttlDays;
    }

    // Shorten response

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShortenResponse {
        private String shortUrl;
        private String shortCode;
        private String longUrl;
        private Instant createdAt;
        private Instant expiresAt;
    }

    // Analytics response

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsResponse {
        private String shortCode;
        private String longUrl;
        private long totalClicks;
        private Instant createdAt;
        private Instant expiresAt;
        private List<DailyClicks> dailyClicks;
    }

    @Data
    @AllArgsConstructor
    public static class DailyClicks {
        private String date;
        private long clicks;
    }
}