package com.urlshortener.AnalyticsService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class AnalyticsDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsResponse {
        private String shortCode;
        private String longUrl;
        private long totalClicks;
        private long uniqueVisitors;
        private Instant createdAt;
        private Instant expiresAt;
        private Instant lastClickedAt;
        private List<DailyClicks> dailyClicks;
        private List<TopReferers> topReferers;
    }

    @Data
    @AllArgsConstructor
    public static class DailyClicks {
        private String date;
        private long clicks;
    }

    @Data
    @AllArgsConstructor
    public static class TopReferers {
        private String referer;
        private long clicks;
    }
}