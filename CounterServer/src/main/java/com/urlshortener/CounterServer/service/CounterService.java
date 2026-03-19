package com.urlshortener.CounterServer.service;

import com.urlshortener.CounterServer.client.RangeServerClient;
import com.urlshortener.CounterServer.client.RangeServerClient.RangeResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CounterService {

    private final RangeServerClient rangeClient;
    private final long refillThreshold;

    private final AtomicLong counter = new AtomicLong(0);
    private volatile long rangeEnd = 0;

    private volatile RangeResponse nextRange = null;
    private final AtomicBoolean prefetching = new AtomicBoolean(false);

    public CounterService(
            RangeServerClient rangeClient,
            @Value("${counter.range-refill-threshold:10000}") long refillThreshold) {
        this.rangeClient = rangeClient;
        this.refillThreshold = refillThreshold;
    }

    @PostConstruct
    public void init() {
        log.info("Counter server starting — fetching initial range...");
        loadRange(rangeClient.fetchNextRange());
    }

    public synchronized long next() {
        long value = counter.get();

        // Range fully exhausted — swap in the pre-fetched range (or fetch now if not ready)
        if (value >= rangeEnd) {
            if (nextRange != null) {
                log.info("Swapping in pre-fetched range [{}, {})", nextRange.getRangeStart(), nextRange.getRangeEnd());
                loadRange(nextRange);
                nextRange = null;
            } else {
                log.warn("Pre-fetched range not ready — fetching synchronously");
                loadRange(rangeClient.fetchNextRange());
            }
        }

        value = counter.getAndIncrement();

        // When remaining drops below threshold, kick off background pre-fetch
        long remaining = rangeEnd - counter.get();
        if (remaining <= refillThreshold && prefetching.compareAndSet(false, true)) {
            prefetchAsync();
        }

        return value;
    }

    private void prefetchAsync() {
        Thread.ofVirtual().name("range-prefetch").start(() -> {
            try {
                log.info("Pre-fetching next range in background (remaining={})", rangeEnd - counter.get());
                nextRange = rangeClient.fetchNextRange();
                log.info("Pre-fetch complete — range [{}, {}) is ready", nextRange.getRangeStart(), nextRange.getRangeEnd());
            } catch (Exception e) {
                log.warn("Pre-fetch failed — will retry on exhaustion: {}", e.getMessage());
            } finally {
                prefetching.set(false);
            }
        });
    }

    private void loadRange(RangeResponse range) {
        counter.set(range.getRangeStart());
        rangeEnd = range.getRangeEnd();
    }

    public long getCurrentCounter() {
        return counter.get();
    }

    public long getRangeEnd() {
        return rangeEnd;
    }

    public long getRemaining() {
        return rangeEnd - counter.get();
    }
}