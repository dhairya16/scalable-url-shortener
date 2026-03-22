package com.urlshortener.RangeServer.service;

import com.urlshortener.RangeServer.model.RangeAssignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class RangeService {

    private final long rangeSize;
    private final long totalSequences;

    // This pointer always points to where the next range starts
    private final AtomicLong nextRangeStart = new AtomicLong(0);

    // Keeps a record of every assignment ever made (for debugging/status)
    private final Map<Long, RangeAssignment> assignments = new ConcurrentHashMap<>();

    public RangeService(@Value("${range.size}") long rangeSize, @Value("${range.total-sequences}") long totalSequences) {
        this.rangeSize = rangeSize;
        this.totalSequences = totalSequences;
        log.info("Range server ready — total={}, rangeSize={}, maxRanges={}",
                totalSequences, rangeSize, totalSequences / rangeSize);
    }

    public synchronized RangeAssignment assignRange(String serverId) {
        // Check if this server already has an active range
        // Re-assign it instead of giving a new one
        Optional<RangeAssignment> existing = assignments.values().stream()
                .filter(a -> a.getAssignedTo().equals(serverId))
                .findFirst();

        if (existing.isPresent()) {
            log.info("Re-assigning existing range [{}, {}) to recovering server '{}'",
                    existing.get().getRangeStart(),
                    existing.get().getRangeEnd(),
                    serverId);
            return existing.get();
        }

        // No existing range — assign a new one
        long start = nextRangeStart.get();
        if (start >= totalSequences) {
            throw new RangesExhaustedException("All sequence ranges are exhausted");
        }

        long end = Math.min(start + rangeSize, totalSequences);
        nextRangeStart.set(end);

        RangeAssignment assignment = RangeAssignment.builder()
                .rangeStart(start)
                .rangeEnd(end)
                .assignedTo(serverId)
                .assignedAtMs(System.currentTimeMillis())
                .build();

        assignments.put(start, assignment);
        log.info("Assigned new range [{}, {}) to server '{}' — total issued: {}",
                start, end, serverId, assignments.size());

        return assignment;
    }

    public Map<Long, RangeAssignment> getAllAssignments() {
        return Map.copyOf(assignments);
    }

    public double getUtilizationPct() {
        return (double) nextRangeStart.get() / totalSequences * 100;
    }

    public long getRangesIssued() {
        return assignments.size();
    }

    public long getTotalRanges() {
        return totalSequences / rangeSize;
    }

    public long getNextRangeStart() {
        return nextRangeStart.get();
    }


    public static class RangesExhaustedException extends RuntimeException {
        public RangesExhaustedException(String msg) {
            super(msg);
        }
    }
}