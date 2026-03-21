package com.urlshortener.AnalyticsService.service;

import com.urlshortener.AnalyticsService.dto.AnalyticsDtos.*;
import com.urlshortener.AnalyticsService.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AnalyticsQueryService {

    private final Connection clickHouseConnection;
    private final UrlRepository urlRepository;

    public Optional<AnalyticsResponse> getAnalytics(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .map(url -> AnalyticsResponse.builder()
                        .shortCode(shortCode)
                        .longUrl(url.getLongUrl())
                        .createdAt(url.getCreatedAt())
                        .expiresAt(url.getExpiresAt())
                        .totalClicks(queryTotalClicks(shortCode))
                        .uniqueVisitors(queryUniqueVisitors(shortCode))
                        .lastClickedAt(queryLastClickedAt(shortCode))
                        .dailyClicks(queryDailyClicks(shortCode))
                        .topReferers(queryTopReferers(shortCode))
                        .build()
                );
    }

    private long queryTotalClicks(String shortCode) {
        String sql = "SELECT count() FROM click_events WHERE short_code = ?";
        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            log.warn("Failed to query total clicks: {}", e.getMessage());
            return 0;
        }
    }

    private long queryUniqueVisitors(String shortCode) {
        String sql = "SELECT uniq(ip_address) FROM click_events WHERE short_code = ?";
        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Exception e) {
            log.warn("Failed to query unique visitors: {}", e.getMessage());
            return 0;
        }
    }

    private Instant queryLastClickedAt(String shortCode) {
        String sql = "SELECT max(clicked_at) FROM click_events WHERE short_code = ?";
        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                var ts = rs.getTimestamp(1);
                return ts != null ? ts.toInstant() : null;
            }
        } catch (Exception e) {
            log.warn("Failed to query last clicked at: {}", e.getMessage());
        }
        return null;
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
            log.warn("Failed to query daily clicks: {}", e.getMessage());
        }
        return result;
    }

    private List<TopReferers> queryTopReferers(String shortCode) {
        String sql = """
                SELECT
                    referer,
                    count() AS clicks
                FROM click_events
                WHERE short_code = ?
                  AND referer != ''
                GROUP BY referer
                ORDER BY clicks DESC
                LIMIT 5
                """;
        List<TopReferers> result = new ArrayList<>();
        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(new TopReferers(
                        rs.getString("referer"),
                        rs.getLong("clicks")
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to query top referers: {}", e.getMessage());
        }
        return result;
    }
}