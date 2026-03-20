package com.urlshortener.UrlShortenerService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage {
    private String shortCode;
    private String longUrl;
    private Instant clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referer;
}
