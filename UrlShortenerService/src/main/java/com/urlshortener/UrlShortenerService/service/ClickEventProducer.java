package com.urlshortener.UrlShortenerService.service;

import com.urlshortener.UrlShortenerService.config.KafkaConfig;
import com.urlshortener.UrlShortenerService.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventProducer {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    public void publish(ClickEventMessage event) {
        CompletableFuture<SendResult<String, ClickEventMessage>> future =
                kafkaTemplate.send(
                        KafkaConfig.CLICK_EVENTS_TOPIC,
                        event.getShortCode(),   // partition key
                        event                   // message value
                );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("Failed to publish click event for '{}': {}",
                        event.getShortCode(), ex.getMessage());
            } else {
                log.debug("Published click event for '{}' to partition {}",
                        event.getShortCode(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
