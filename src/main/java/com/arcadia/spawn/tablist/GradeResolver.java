package com.arcadia.spawn.tablist;

import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves a player's display "grade" (rank/group) using LuckPerms when present,
 * or falling back to vanilla op level / a single default group when not.
 *
 * Output is a {@link Grade} record with:
 *   - id       : machine-safe group id (lowercase, used for team name)
 *   - display  : human-readable label (uses LuckPerms display name if set)
 *   - weight   : numeric weight, higher = higher rank (LP weight, or 1000-op for ops, or 0 default)
 *   - prefix   : raw prefix string (LuckPerms meta) — may contain '&'-style codes; empty when none
 *   - color    : suggested name color hex ARGB int, or -1 if no preference
 *
 * Resolution is cached lazily per call — callers should re-resolve on PlayerLoggedIn,
 * UserDataRecalculateEvent (LP), or the tablist refresh tick. No long-term cache here.
 */
public final class GradeResolver {

    private GradeResolver() {}

    public record Grade(String id, String display, int weight, String prefix, int color) {
        public static final Grade UNKNOWN = new Grade("default", "Default", 0, "", -1);
    }

    private static final boolean LUCKPERMS_AVAILABLE = isLuckPermsPresent();

    private static boolean isLuckPermsPresent() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean hasLuckPerms() {
        return LUCKPERMS_AVAILABLE;
    }

    public static Grade resolve(ServerPlayer player) {
        if (player == null) return Grade.UNKNOWN;

        if (LUCKPERMS_AVAILABLE) {
            Grade lp = LuckPermsReader.read(player);
            if (lp != null) return lp;
        }

        // Vanilla fallback — op level → synthesized grade.
        // Server permissions level (1..4). 0 = no op.
        int op = 0;
        try {
            op = player.getServer().getProfilePermissions(player.getGameProfile());
        } catch (Exception ignored) {}

        if (op >= 4) return new Grade("op4", "Owner", 1000, "&4[Owner] ", 0xFFAA0000);
        if (op == 3) return new Grade("op3", "Admin", 900, "&c[Admin] ", 0xFFFF5555);
        if (op == 2) return new Grade("op2", "Mod",   800, "&5[Mod] ",   0xFFAA00AA);
        if (op == 1) return new Grade("op1", "Helper",700, "&3[Helper] ",0xFF00AAAA);
        return Grade.UNKNOWN;
    }

    /**
     * Isolated so the JVM only loads LuckPerms classes when LP is actually present.
     */
    private static final class LuckPermsReader {
        static Grade read(ServerPlayer player) {
            try {
                var lp = net.luckperms.api.LuckPermsProvider.get();
                var user = lp.getUserManager().getUser(player.getUUID());
                if (user == null) return null;

                String primaryName = user.getPrimaryGroup();
                var groupManager = lp.getGroupManager();
                var group = groupManager.getGroup(primaryName);

                int weight = 0;
                String display = primaryName;
                if (group != null) {
                    weight = group.getWeight().orElse(0);
                    String dn = group.getDisplayName();
                    if (dn != null && !dn.isBlank()) display = dn;
                }

                var meta = user.getCachedData().getMetaData();
                String prefix = meta.getPrefix();
                if (prefix == null) prefix = "";

                int color = -1;
                String colorMeta = meta.getMetaValue("color");
                if (colorMeta != null) {
                    try {
                        // accept #RRGGBB or 0xAARRGGBB
                        String c = colorMeta.startsWith("#") ? colorMeta.substring(1) : colorMeta.replace("0x", "");
                        long parsed = Long.parseLong(c, 16);
                        if (c.length() <= 6) parsed |= 0xFF000000L;
                        color = (int) parsed;
                    } catch (NumberFormatException ignored) {}
                }

                String safeId = primaryName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                return new Grade(safeId, display, weight, prefix, color);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
