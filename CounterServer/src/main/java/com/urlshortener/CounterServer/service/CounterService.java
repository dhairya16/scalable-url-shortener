package com.urlshortener.CounterServer.service;

import com.urlshortener.CounterServer.client.RangeServerClient;
import com.urlshortener.CounterServer.client.RangeServerClient.RangeResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CounterService {

    private final RangeServerClient rangeClient;
    private final StringRedisTemplate redis;
    private final long refillThreshold;
    private final String serverId;

    private final AtomicLong counter = new AtomicLong(0);
    private volatile long rangeEnd = 0;
    private volatile RangeResponse nextRange = null;
    private final AtomicBoolean prefetching = new AtomicBoolean(false);

    // Redis key format: "counter:last:counter-server-1"
    private String redisKey() {
        return "counter:last:" + serverId;
    }

    public CounterService(
            RangeServerClient rangeClient,
            StringRedisTemplate redis,
            @Value("${counter.server-id}") String serverId,
            @Value("${counter.range-refill-threshold:10000}") long refillThreshold) {
        this.rangeClient = rangeClient;
        this.redis = redis;
        this.serverId = serverId;
        this.refillThreshold = refillThreshold;
    }

    @PostConstruct
    public void init() {
        log.info("Counter server '{}' starting...", serverId);

        // Check if we have a saved position from before a crash
        String saved = redis.opsForValue().get(redisKey());

        if (saved != null) {
            long lastUsed = Long.parseLong(saved);
            log.info("Found saved counter position in Redis: {}", lastUsed);

            // Ask Range Server for a new range as usual
            RangeResponse range = rangeClient.fetchNextRange();

            // If last used value falls within this new range, resume from there
            // This handles the case where we crashed mid-range
            if (lastUsed >= range.getRangeStart() && lastUsed < range.getRangeEnd()) {
                long resumeFrom = lastUsed + 1;
                counter.set(resumeFrom);
                rangeEnd = range.getRangeEnd();
                log.info("Resuming from saved position {} within range [{}, {})",
                        resumeFrom, range.getRangeStart(), range.getRangeEnd());
                return;
            }

            // Last used value is outside this range — start fresh from rangeStart
            log.info("Saved position {} is outside new range [{}, {}) — starting fresh",
                    lastUsed, range.getRangeStart(), range.getRangeEnd());
            loadRange(range);
        } else {
            // First ever start — no saved position
            log.info("No saved position found — fetching initial range");
            loadRange(rangeClient.fetchNextRange());
        }
    }

    public synchronized long next() {
        // Range exhausted — switch to next
        if (counter.get() >= rangeEnd) {
            if (nextRange != null) {
                log.info("Swapping in pre-fetched range [{}, {})",
                        nextRange.getRangeStart(), nextRange.getRangeEnd());
                loadRange(nextRange);
                nextRange = null;
            } else {
                log.warn("Pre-fetched range not ready — fetching synchronously");
                loadRange(rangeClient.fetchNextRange());
            }
        }

        long value = counter.getAndIncrement();

        // Persist to Redis after every increment
        // This is the crash recovery checkpoint
        redis.opsForValue().set(redisKey(), String.valueOf(value));

        // Pre-fetch next range when running low
        long remaining = rangeEnd - counter.get();
        if (remaining <= refillThreshold && prefetching.compareAndSet(false, true)) {
            prefetchAsync();
        }

        return value;
    }

    private void prefetchAsync() {
        Thread.ofVirtual().name("range-prefetch").start(() -> {
            try {
                log.info("Pre-fetching next range in background...");
                nextRange = rangeClient.fetchNextRange();
                log.info("Pre-fetch complete — range [{}, {}) ready",
                        nextRange.getRangeStart(), nextRange.getRangeEnd());
            } catch (Exception e) {
                log.warn("Pre-fetch failed: {}", e.getMessage());
            } finally {
                prefetching.set(false);
            }
        });
    }

    private void loadRange(RangeResponse range) {
        counter.set(range.getRangeStart());
        rangeEnd = range.getRangeEnd();
        log.info("Loaded range [{}, {})", range.getRangeStart(), range.getRangeEnd());
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