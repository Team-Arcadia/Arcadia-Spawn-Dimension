package com.arcadia.spawn.tablist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of named animation frame lists. Each animation is a cyclic list of
 * strings ("frames") that the placeholder formatter rotates through based on a
 * monotonic frame counter.
 *
 * Frames can contain '&'-style color codes — they are translated by the
 * formatter just like any other template line.
 *
 * Defaults ship with eye-candy animations referenced by the default header
 * and footer templates; users can override or add their own in the config.
 */
public final class AnimationFrames {

    private static final Map<String, List<String>> FRAMES = new HashMap<>();
    private static volatile long frameCounter = 0L;

    private AnimationFrames() {}

    static {
        // Pulsing accent dot — useful as a section bullet or status indicator.
        FRAMES.put("pulse", List.of(
                "&8●", "&7●", "&f●", "&b●", "&f●", "&7●"
        ));

        // Loading-style dots, 3 frames for a smooth left-to-right motion.
        FRAMES.put("loading", List.of(
                "&a● &7● &7●",
                "&7● &a● &7●",
                "&7● &7● &a●",
                "&7● &a● &7●"
        ));

        // Rainbow color cycle for a single character (used as a separator accent).
        FRAMES.put("rainbow", List.of(
                "&c●", "&6●", "&e●", "&a●", "&b●", "&d●"
        ));

        // Title color cycle — applied as a prefix to the server name.
        FRAMES.put("title_color", List.of(
                "&b&l", "&3&l", "&b&l", "&f&l", "&b&l", "&3&l"
        ));

        // Slow scrolling separator — a sliding bright spot in a wide dark bar.
        // 80 chars wide so it spans the full width of the tab list even for the
        // widest layouts (long player names + long LP prefixes).
        FRAMES.put("scroll", List.of(
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━&b━&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        ));

        // Full-width separator bar (forces a wider tab list).
        FRAMES.put("bar", List.of(
                "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        ));

        // Online/Active blink — switches color of an indicator every 2 frames.
        FRAMES.put("blink", List.of(
                "&a▌", "&a▌", "&2▌", "&2▌"
        ));
    }

    /**
     * Advance the global frame counter. Called from the refresh tick — once per
     * tab list refresh cycle, regardless of how many players or peers there are.
     */
    public static void advance() {
        frameCounter++;
    }

    /** Override or add an animation frame list from config. */
    public static void register(String name, List<String> frames) {
        if (name == null || frames == null || frames.isEmpty()) return;
        FRAMES.put(name.toLowerCase(), frames);
    }

    public static void clearOverrides() {
        // Nothing to do — registrations from config just overwrite defaults.
    }

    /**
     * Resolve %anim_<name>% to the current frame of the named animation.
     * Returns the literal placeholder text when unknown so the user sees what's wrong.
     */
    public static String resolve(String name) {
        if (name == null) return "";
        List<String> frames = FRAMES.get(name.toLowerCase());
        if (frames == null || frames.isEmpty()) return "%anim_" + name + "%";
        int idx = (int) Math.floorMod(frameCounter, frames.size());
        return frames.get(idx);
    }

    public static long currentFrame() {
        return frameCounter;
    }
}
