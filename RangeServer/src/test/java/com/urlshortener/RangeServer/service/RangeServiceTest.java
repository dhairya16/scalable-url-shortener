package com.urlshortener.RangeServer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RangeService")
class RangeServiceTest {

    private RangeService rangeService;

    private static final long RANGE_SIZE = 1_000_000L;
    private static final long TOTAL_SEQUENCES = 20_000_000L;

    @BeforeEach
    void setUp() {
        rangeService = new RangeService(RANGE_SIZE, TOTAL_SEQUENCES);
    }

    @Test
    @DisplayName("first assignment starts at 0")
    void firstAssignment_startsAtZero() {
        var assignment = rangeService.assignRange("server-1");
        assertEquals(0, assignment.getRangeStart());
    }

    @Test
    @DisplayName("each assignment starts where previous ended")
    void consecutiveAssignments_areContiguous() {
        var first = rangeService.assignRange("server-1");
        var second = rangeService.assignRange("server-2");

        assertEquals(first.getRangeEnd(), second.getRangeStart(),
                "Second range should start exactly where first ended");
    }

    @Test
    @DisplayName("assignments never overlap")
    void assignments_neverOverlap() {
        var first = rangeService.assignRange("server-1");
        var second = rangeService.assignRange("server-2");
        var third = rangeService.assignRange("server-3");

        assertTrue(first.getRangeEnd() <= second.getRangeStart());
        assertTrue(second.getRangeEnd() <= third.getRangeStart());
    }

    @Test
    @DisplayName("range size matches configured size")
    void assignment_hasCorrectRangeSize() {
        var assignment = rangeService.assignRange("server-1");
        assertEquals(RANGE_SIZE, assignment.getRangeEnd() - assignment.getRangeStart());
    }

    @Test
    @DisplayName("assignedTo field matches requesting server")
    void assignment_recordsCorrectServerId() {
        var assignment = rangeService.assignRange("counter-server-42");
        assertEquals("counter-server-42", assignment.getAssignedTo());
    }

    @Test
    @DisplayName("re-assigning same server returns existing range")
    void assignRange_sameServer_returnsExistingRange() {
        var first = rangeService.assignRange("server-1");
        var second = rangeService.assignRange("server-1");

        assertEquals(first.getRangeStart(), second.getRangeStart(),
                "Same server should get the same range back");
    }

    @Test
    @DisplayName("throws when all ranges are exhausted")
    void assignRange_whenExhausted_throwsException() {
        // Exhaust ALL ranges — TOTAL_SEQUENCES / RANGE_SIZE = 20
        long totalRanges = TOTAL_SEQUENCES / RANGE_SIZE;
        for (int i = 0; i < totalRanges; i++) {
            rangeService.assignRange("server-" + i);
        }
        assertThrows(RangeService.RangesExhaustedException.class,
                () -> rangeService.assignRange("server-overflow"));
    }

    @Test
    @DisplayName("concurrent assignments produce no duplicates")
    void assignRange_concurrent_producesNoDuplicates() throws InterruptedException {
        int threadCount = 10; // stay within our 10-range limit
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Set<Long> assignedStarts = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            final int serverId = i;
            executor.submit(() -> {
                try {
                    var assignment = rangeService.assignRange("server-" + serverId);
                    assignedStarts.add(assignment.getRangeStart());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount, assignedStarts.size(),
                "Each thread should have received a unique range start — found duplicates");
    }

    @Test
    @DisplayName("utilization increases with each assignment")
    void utilization_increasesWithEachAssignment() {
        double before = rangeService.getUtilizationPct();
        rangeService.assignRange("server-1");
        double after = rangeService.getUtilizationPct();
        assertTrue(after > before);
    }
}