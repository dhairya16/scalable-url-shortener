package com.urlshortener.CounterServer.controller;

import com.urlshortener.CounterServer.service.CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/counter")
@RequiredArgsConstructor
public class CounterController {

    private final CounterService counterService;

    @GetMapping("/next")
    public ResponseEntity<Map<String, Long>> next() {
        return ResponseEntity.ok(Map.of("value", counterService.next()));
    }

    // for debugging purpose
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "currentCounter", counterService.getCurrentCounter(),
                "rangeEnd", counterService.getRangeEnd(),
                "remaining", counterService.getRemaining()
        ));
    }
}
