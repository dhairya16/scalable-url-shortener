package com.urlshortener.CounterServer.client;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class RangeServerClient {

    private final RestTemplate restTemplate;
    private final String rangeServerUrl;
    private final String serverId;

    public RangeServerClient(
            @Value("${range-server.url}") String rangeServerUrl,
            @Value("${counter.server-id}") String serverId) {
        this.restTemplate = new RestTemplate();
        this.rangeServerUrl = rangeServerUrl;
        this.serverId = serverId;
    }

    public RangeResponse fetchNextRange() {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(rangeServerUrl + "/range/assign")
                .queryParam("serverId", serverId)
                .build()
                .toUri();

        log.info("Requesting new range from Range Server for '{}'", serverId);
        RangeResponse response = restTemplate.postForObject(uri, null, RangeResponse.class);

        if (response == null) {
            throw new RuntimeException("Null response from Range Server");
        }

        log.info("Got range [{}, {})", response.getRangeStart(), response.getRangeEnd());
        return response;
    }

    @Getter
    @Setter
    public static class RangeResponse {
        private long rangeStart;
        private long rangeEnd;
        private String assignedTo;
        private long assignedAtMs;

    }
}