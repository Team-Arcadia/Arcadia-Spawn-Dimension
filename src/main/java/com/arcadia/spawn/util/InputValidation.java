package com.arcadia.spawn.util;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Strict input validation for user-supplied identifiers and free-form text.
 * Prevents path traversal, filename injection, oversize payloads.
 */
public final class InputValidation {

    private InputValidation() {}

    // Lobby names: alphanumeric, underscore, dash. 1–32 chars.
    private static final Pattern LOBBY_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]{1,32}$");

    // Dimension custom IDs: lowercase alphanumeric + underscore. 3–32 chars.
    private static final Pattern DIM_ID = Pattern.compile("^[a-z0-9_]{3,32}$");

    // Description: 256 chars max, no control chars.
    public static final int MAX_DESCRIPTION = 256;

    // Windows reserved filenames — must be rejected even if regex passes.
    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

    public static boolean isValidLobbyName(String name) {
        if (name == null) return false;
        if (!LOBBY_NAME.matcher(name).matches()) return false;
        return !WINDOWS_RESERVED.contains(name.toUpperCase(java.util.Locale.ROOT));
    }

    public static boolean isValidDimensionId(String id) {
        if (id == null) return false;
        if (!DIM_ID.matcher(id).matches()) return false;
        return !WINDOWS_RESERVED.contains(id.toUpperCase(java.util.Locale.ROOT));
    }

    public static String sanitizeDescription(String desc) {
        if (desc == null) return "";
        String trimmed = desc.length() > MAX_DESCRIPTION ? desc.substring(0, MAX_DESCRIPTION) : desc;
        // Strip control characters except common whitespace
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 32 || c == '\t') out.append(c);
        }
        return out.toString();
    }
}
