package com.urlshortener.UrlShortenerService.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CounterServerClient {

    private final List<String> serverUrls;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AtomicInteger roundRobin = new AtomicInteger(0);

    public CounterServerClient(
            @Value("${app.counter-server-urls}") String urlsCsv) {
        this.serverUrls = Arrays.stream(urlsCsv.split(","))
                .map(String::trim)
                .toList();
        log.info("Counter server pool: {}", serverUrls);
    }

    public long nextSequence() {
        String url = nextServerUrl() + "/counter/next";
        log.debug("Fetching sequence from {}", url);

        @SuppressWarnings("unchecked")
        var response = (java.util.Map<String, Object>)
                restTemplate.getForObject(url, java.util.Map.class);

        if (response == null || !response.containsKey("value")) {
            throw new RuntimeException("Invalid response from counter server: " + url);
        }

        return ((Number) response.get("value")).longValue();
    }

    private String nextServerUrl() {
        int idx = Math.abs(roundRobin.getAndIncrement() % serverUrls.size());
        return serverUrls.get(idx);
    }
}