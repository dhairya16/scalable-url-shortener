package com.urlshortener.UrlShortenerService.service;


import com.urlshortener.UrlShortenerService.dto.UrlDtos.AnalyticsResponse;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.DailyClicks;
import com.urlshortener.UrlShortenerService.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final Connection clickHouseConnection;
    private final UrlRepository urlRepository;

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));


    @Async("analyticsExecutor")
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        String sql = """
                INSERT INTO click_events (short_code, clicked_at, ip_address, user_agent, referer)
                VALUES (?, now(), ?, ?, ?)
                """;

        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            stmt.setString(2, ipAddress);
            stmt.setString(3, userAgent != null ? userAgent : "");
            stmt.setString(4, referer != null ? referer : "");
            stmt.executeUpdate();
            log.debug("Recorded click for '{}'", shortCode);
        } catch (Exception e) {
            log.warn("Failed to record click for '{}': {}", shortCode, e.getMessage());
        }
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
                    toDate(clicked_at)  AS day,
                    count()             AS clicks
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