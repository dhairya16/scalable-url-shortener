package com.urlshortener.UrlRedirectService.service;

import com.urlshortener.UrlRedirectService.config.KafkaConfig;
import com.urlshortener.UrlRedirectService.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventProducer {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    public void publish(ClickEventMessage event) {
        kafkaTemplate.send(
                KafkaConfig.CLICK_EVENTS_TOPIC,
                event.getShortCode(),
                event
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("Failed to publish click for '{}': {}",
                        event.getShortCode(), ex.getMessage());
            } else {
                log.debug("Published click for '{}' to partition {}",
                        event.getShortCode(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}