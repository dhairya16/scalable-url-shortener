package com.urlshortener.AnalyticsService.controller;


import com.urlshortener.AnalyticsService.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> getAnalytics(@PathVariable String shortCode) {
        return analyticsService.getAnalytics(shortCode)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", 404,
                                "error", "Not Found",
                                "message", "Short code '" + shortCode + "' does not exist",
                                "timestamp", Instant.now().toString()
                        )));
    }
}
