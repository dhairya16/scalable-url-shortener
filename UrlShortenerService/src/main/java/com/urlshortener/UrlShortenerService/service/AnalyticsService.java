package com.urlshortener.UrlShortenerService.service;

import com.urlshortener.UrlShortenerService.dto.ClickEventMessage;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.AnalyticsResponse;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.DailyClicks;
import com.urlshortener.UrlShortenerService.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickEventProducer producer;
    private final Connection clickHouseConnection;
    private final UrlRepository urlRepository;

    @Async("analyticsExecutor")
    public void recordClick(String shortCode, String longUrl,
                            String ipAddress, String userAgent, String referer) {
        ClickEventMessage event = ClickEventMessage.builder()
                .shortCode(shortCode)
                .longUrl(longUrl)
                .clickedAt(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent != null ? userAgent : "")
                .referer(referer != null ? referer : "")
                .build();

        producer.publish(event);
    }

    public Optional<AnalyticsResponse> getAnalytics(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .map(url -> {
                    long totalClicks = queryTotalClicks(shortCode);
                    List<DailyClicks> daily = queryDailyClicks(shortCode);

                    return AnalyticsResponse.builder()
                            .shortCode(shortCode)
                            .longUrl(url.getLongUrl())
                            .totalClicks(totalClicks)
                            .createdAt(url.getCreatedAt())
                            .expiresAt(url.getExpiresAt())
                            .dailyClicks(daily)
                            .build();
                });
    }

    private long queryTotalClicks(String shortCode) {
        String sql = "SELECT count() FROM click_events WHERE short_code = ?";
        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            log.warn("Failed to query total clicks for '{}': {}", shortCode, e.getMessage());
            return 0;
        }
    }

    private List<DailyClicks> queryDailyClicks(String shortCode) {
        String sql = """
                SELECT
                    toDate(clicked_at) AS day,
                    count()            AS clicks
                FROM click_events
                WHERE short_code = ?
                  AND clicked_at >= now() - INTERVAL 30 DAY
                GROUP BY day
                ORDER BY day DESC
                """;

        List<DailyClicks> result = new ArrayList<>();
        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new DailyClicks(
                        rs.getString("day"),
                        rs.getLong("clicks")
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to query daily clicks for '{}': {}", shortCode, e.getMessage());
        }
        return result;
    }
}