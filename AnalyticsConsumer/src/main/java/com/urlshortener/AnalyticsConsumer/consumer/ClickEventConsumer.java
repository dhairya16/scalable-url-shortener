package com.urlshortener.AnalyticsConsumer.consumer;

import com.urlshortener.AnalyticsConsumer.dto.ClickEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ClickEventConsumer {

    private final Connection clickHouseConnection;
    private final int batchSize;

    private final List<ClickEventMessage> buffer = new ArrayList<>();

    public ClickEventConsumer(
            Connection clickHouseConnection,
            @Value("${analytics.batch-size:10}") int batchSize) {
        this.clickHouseConnection = clickHouseConnection;
        this.batchSize = batchSize;
    }

    @KafkaListener(
            topics = "click-events",
            groupId = "analytics-consumer-group"
    )
    public void consume(ClickEventMessage event, Acknowledgment ack) {
        buffer.add(event);
        log.debug("Buffered click for '{}' — buffer size: {}/{}",
                event.getShortCode(), buffer.size(), batchSize);

        // Flush to ClickHouse when buffer reaches batch size
        if (buffer.size() >= batchSize) {
            flushToClickHouse();
        }

        // Acknowledge offset to Kafka
        ack.acknowledge();
    }

    private void flushToClickHouse() {
        if (buffer.isEmpty()) return;

        String sql = """
                INSERT INTO click_events
                    (short_code, clicked_at, ip_address, user_agent, referer)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = clickHouseConnection.prepareStatement(sql)) {
            for (ClickEventMessage event : buffer) {
                stmt.setString(1, event.getShortCode());
                stmt.setTimestamp(2, event.getClickedAt() != null
                        ? Timestamp.from(event.getClickedAt())
                        : new Timestamp(System.currentTimeMillis()));
                stmt.setString(3, event.getIpAddress() != null ? event.getIpAddress() : "");
                stmt.setString(4, event.getUserAgent() != null ? event.getUserAgent() : "");
                stmt.setString(5, event.getReferer() != null ? event.getReferer() : "");
                stmt.addBatch();
            }

            stmt.executeBatch();
            log.info("Flushed {} click events to ClickHouse", buffer.size());
            buffer.clear();

        } catch (Exception e) {
            log.error("Failed to flush {} events to ClickHouse: {}",
                    buffer.size(), e.getMessage());
            // retry on next incoming event, keeping buffer as it is
        }
    }

    @Scheduled(fixedDelay = 5000)   // runs every 5 seconds
    public void scheduledFlush() {
        if (!buffer.isEmpty()) {
            log.info("Scheduled flush triggered — flushing {} buffered events", buffer.size());
            flushToClickHouse();
        }
    }
}