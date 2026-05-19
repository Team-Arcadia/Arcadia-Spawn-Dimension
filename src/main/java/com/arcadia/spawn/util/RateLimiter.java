package com.arcadia.spawn.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player, per-key rate limiter using a token bucket.
 * Lightweight: O(1) check, no scheduled tasks. Stale entries auto-evict on access.
 *
 * Used to protect packet handlers from spam (e.g. C2SOpenLobby), and admin
 * commands from being called dozens of times per tick.
 */
public final class RateLimiter {

    private RateLimiter() {}

    private static final Map<String, Long> LAST_CALL = new ConcurrentHashMap<>();
    private static final Map<String, Integer> COUNT = new ConcurrentHashMap<>();

    /**
     * Allows up to `maxPerWindow` calls within `windowMs`. Returns true if allowed.
     */
    public static boolean tryAcquire(UUID uuid, String key, int maxPerWindow, long windowMs) {
        String compoundKey = uuid + ":" + key;
        long now = System.currentTimeMillis();
        Long last = LAST_CALL.get(compoundKey);

        if (last == null || (now - last) > windowMs) {
            LAST_CALL.put(compoundKey, now);
            COUNT.put(compoundKey, 1);
            return true;
        }

        int count = COUNT.merge(compoundKey, 1, Integer::sum);
        return count <= maxPerWindow;
    }

    public static void onDisconnect(UUID uuid) {
        String prefix = uuid + ":";
        LAST_CALL.keySet().removeIf(k -> k.startsWith(prefix));
        COUNT.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
