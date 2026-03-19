package com.urlshortener.RangeServer.controller;

import com.urlshortener.RangeServer.model.RangeAssignment;
import com.urlshortener.RangeServer.service.RangeService;
import com.urlshortener.RangeServer.service.RangeService.RangesExhaustedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/range")
@RequiredArgsConstructor
public class RangeController {

    private final RangeService rangeService;

    @PostMapping("/assign")
    public ResponseEntity<?> assign(@RequestParam String serverId) {
        try {
            RangeAssignment assignment = rangeService.assignRange(serverId);
            return ResponseEntity.ok(assignment);
        } catch (RangesExhaustedException ex) {
            return ResponseEntity.status(503).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "rangesIssued", rangeService.getRangesIssued(),
                "totalRanges", rangeService.getTotalRanges(),
                "nextRangeStart", rangeService.getNextRangeStart()
        ));
    }
}