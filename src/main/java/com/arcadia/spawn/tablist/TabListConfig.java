package com.arcadia.spawn.tablist;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Server-side tab list configuration.
 *
 * Everything is OFF by default — backward compatibility: upgrading from a prior
 * version changes nothing visible until the admin explicitly flips
 * {@code tablist.enabled = true}.
 *
 * Cross-server sync re-uses arcadia-lib's DatabaseManager — no separate DB
 * connection here. If arcadia-lib's DB is configured and active, this mod will
 * heartbeat its own count and read peers automatically.
 */
public class TabListConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    public static class Values {
        // Master toggle
        public final ModConfigSpec.ConfigValue<Boolean> enabled;

        // Display
        public final ModConfigSpec.ConfigValue<String> serverDisplayName;
        public final ModConfigSpec.ConfigValue<List<? extends String>> headerLines;
        public final ModConfigSpec.ConfigValue<List<? extends String>> footerLines;
        public final ModConfigSpec.ConfigValue<Integer> refreshIntervalTicks;

        // Group sorting
        public final ModConfigSpec.ConfigValue<Boolean> groupSortingEnabled;
        public final ModConfigSpec.ConfigValue<Boolean> showLuckPermsPrefix;

        // Spectator hiding
        public final ModConfigSpec.ConfigValue<Boolean> hideSpectatorsFromTab;

        // Cross-server (via arcadia-lib DB)
        public final ModConfigSpec.ConfigValue<Boolean> crossServerEnabled;
        public final ModConfigSpec.ConfigValue<Integer> heartbeatIntervalSeconds;
        public final ModConfigSpec.ConfigValue<Integer> peerTimeoutSeconds;
        public final ModConfigSpec.ConfigValue<List<? extends String>> peerOrder;

        Values(ModConfigSpec.Builder builder) {
            builder.push("TabList");
            builder.comment(
                    "Custom tab list with group-based sorting and cross-server player counts.",
                    "Cross-server uses the arcadia-lib shared database — configure it once",
                    "in arcadia-lib's database config, set the same credentials on all servers,",
                    "and give each server a unique SERVER_ID."
            );

            enabled = builder.comment(
                    "Master toggle. When false, vanilla tab list is left untouched (default).")
                    .define("enabled", false);

            serverDisplayName = builder.comment(
                    "Display name of this server in the tab list footer (e.g. 'Lobby', 'Survie', 'PvP').",
                    "If empty, falls back to arcadia-lib ServerContext.SERVER_ID.")
                    .define("server_display_name", "");

            headerLines = builder.comment(
                    "Header lines — one entry per line. Supports placeholders:",
                    "  %server%, %online%, %max%, %tps%, %mspt%, %uptime%,",
                    "  %player_name%, %player_ping%, %player_playtime%, %lp_group%, %lp_prefix%,",
                    "  %cross_total% (sum across all alive peers).",
                    "Color codes: use '&' (e.g. '&6Welcome &fto &b%server%').")
                    .defineList("header_lines",
                            Arrays.asList(
                                    "",
                                    "&b&lArcadia &8• &f%server%",
                                    "&7%online%&8/&7%max% online &8• &7TPS &a%tps% &8• &7Net &a%cross_total%",
                                    ""
                            ),
                            entry -> entry instanceof String);

            footerLines = builder.comment(
                    "Footer lines — same placeholders as header_lines, plus one extra line per",
                    "cross-server peer when cross_server_enabled is true:",
                    "  the line %peers% is auto-expanded to one row per peer.")
                    .defineList("footer_lines",
                            Arrays.asList(
                                    "",
                                    "%peers%",
                                    "",
                                    "&7Ping &a%player_ping%ms &8• &7Playtime &a%player_playtime%",
                                    "",
                                    "&b&narcadia-echoes-of-power.fr",
                                    ""
                            ),
                            entry -> entry instanceof String);

            refreshIntervalTicks = builder.comment(
                    "Header/footer refresh interval in ticks (20 = 1 second).",
                    "Lower = smoother updates, higher = less overhead. Default 40 (2s).")
                    .defineInRange("refresh_interval_ticks", 40, 5, 600);

            builder.pop();

            builder.push("Group Sorting");
            groupSortingEnabled = builder.comment(
                    "Sort players in the tab list by LuckPerms group weight (highest first).",
                    "Without LuckPerms: sort by op level (op4 first, default last).")
                    .define("group_sorting_enabled", true);

            showLuckPermsPrefix = builder.comment(
                    "Prefix the player name with their LuckPerms group prefix (e.g. '[VIP] Player').",
                    "Has no effect without LuckPerms.")
                    .define("show_luckperms_prefix", true);

            hideSpectatorsFromTab = builder.comment(
                    "Hide players in vanilla Spectator gamemode from the tab list of normal players.",
                    "Spectators still see each other in the tab so co-moderation works.")
                    .define("hide_spectators_from_tab", true);
            builder.pop();

            builder.push("Cross-Server Sync");
            builder.comment(
                    "Requires arcadia-lib database to be enabled and reachable.",
                    "Each server writes its own row into the `arcadia_tablist_servers` table",
                    "every `heartbeat_interval_seconds`, and reads the others on each refresh.");

            crossServerEnabled = builder.comment(
                    "Enable cross-server player count display.",
                    "Requires arcadia-lib DB enabled — falls back gracefully when DB is down.")
                    .define("cross_server_enabled", false);

            heartbeatIntervalSeconds = builder.comment(
                    "How often this server pushes its own state to the shared DB (seconds).")
                    .defineInRange("heartbeat_interval_seconds", 10, 5, 120);

            peerTimeoutSeconds = builder.comment(
                    "A peer not heartbeating for this many seconds is considered offline.",
                    "Should be 2-3x heartbeat_interval_seconds.")
                    .defineInRange("peer_timeout_seconds", 45, 15, 600);

            peerOrder = builder.comment(
                    "Optional explicit ordering of peers in the footer.",
                    "List of SERVER_IDs in display order. Unknown peers are appended at the end.")
                    .defineList("peer_order",
                            List.of(),
                            entry -> entry instanceof String);
            builder.pop();
        }
    }

    static {
        Pair<Values, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Values::new);
        SPEC = specPair.getRight();
        VALUES = specPair.getLeft();
    }
}
