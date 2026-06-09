package com.arcadia.spawn.tablist;

import com.arcadia.spawn.tablist.CrossServerDb.PeerSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Formats tab list header/footer template lines, expanding placeholders and
 * '&'-style color codes.
 *
 * Placeholders:
 *   %server%             — server display name
 *   %online%, %max%      — local player count
 *   %tps%, %mspt%        — server performance
 *   %uptime%             — server uptime (e.g. "3h 14m")
 *   %player_name%, %player_ping%, %player_playtime%
 *   %lp_group%, %lp_prefix%
 *   %cross_total%        — sum of online across alive peers (including self)
 *   %peers%              — special multi-line expansion: one row per peer
 */
public final class PlaceholderFormatter {

    private PlaceholderFormatter() {}

    public static Component formatLines(List<? extends String> templates,
                                        ServerPlayer player,
                                        MinecraftServer server,
                                        String serverDisplayName,
                                        List<PeerSnapshot> peers) {
        MutableComponent out = Component.literal("");
        boolean first = true;
        for (String line : templates) {
            if (line == null) continue;
            if (line.contains("%peers%")) {
                for (Component peerLine : expandPeers(peers)) {
                    if (!first) out.append("\n");
                    out.append(peerLine);
                    first = false;
                }
                continue;
            }
            String expanded = expand(line, player, server, serverDisplayName, peers);
            if (!first) out.append("\n");
            out.append(translateColors(expanded));
            first = false;
        }
        return out;
    }

    private static final java.util.regex.Pattern ANIM_PATTERN =
            java.util.regex.Pattern.compile("%anim_([a-zA-Z0-9_]+)%");

    private static String expand(String line, ServerPlayer player, MinecraftServer server,
                                 String serverDisplayName, List<PeerSnapshot> peers) {
        String result = line;

        // Animations first so subsequent replacements can apply on their output if needed.
        java.util.regex.Matcher m = ANIM_PATTERN.matcher(result);
        if (m.find()) {
            StringBuilder sb = new StringBuilder();
            m.reset();
            while (m.find()) {
                m.appendReplacement(sb,
                        java.util.regex.Matcher.quoteReplacement(AnimationFrames.resolve(m.group(1))));
            }
            m.appendTail(sb);
            result = sb.toString();
        }

        // Each replace() that runs allocates a new String even when the placeholder is absent,
        // and the value-producing helpers (formatTps/Mspt/Uptime, GradeResolver.resolve →
        // LuckPerms lookup) are not free. This runs once per online player every refresh tick,
        // so we guard every replacement on a cheap contains() scan and only compute a value
        // when its placeholder is actually present in the line.
        if (result.contains("%server%"))  result = result.replace("%server%", serverDisplayName);
        if (result.contains("%online%"))  result = result.replace("%online%", String.valueOf(server.getPlayerList().getPlayerCount()));
        if (result.contains("%max%"))     result = result.replace("%max%", String.valueOf(server.getMaxPlayers()));
        if (result.contains("%tps%"))     result = result.replace("%tps%", formatTps(server));
        if (result.contains("%mspt%"))    result = result.replace("%mspt%", formatMspt(server));
        if (result.contains("%uptime%"))  result = result.replace("%uptime%", formatUptime(server));

        if (player != null) {
            if (result.contains("%player_name%"))     result = result.replace("%player_name%", player.getName().getString());
            if (result.contains("%player_ping%"))     result = result.replace("%player_ping%", String.valueOf(player.connection != null ? player.connection.latency() : 0));
            if (result.contains("%player_playtime%")) result = result.replace("%player_playtime%", formatPlaytime(player));

            // Resolve the grade (a LuckPerms cache traversal) only when a grade placeholder is present.
            if (result.contains("%lp_group%") || result.contains("%lp_prefix%")) {
                GradeResolver.Grade grade = GradeResolver.resolve(player);
                if (result.contains("%lp_group%"))  result = result.replace("%lp_group%", grade.display());
                if (result.contains("%lp_prefix%")) result = result.replace("%lp_prefix%", grade.prefix());
            }
        } else {
            if (result.contains("%player_name%"))     result = result.replace("%player_name%", "");
            if (result.contains("%player_ping%"))     result = result.replace("%player_ping%", "0");
            if (result.contains("%player_playtime%")) result = result.replace("%player_playtime%", "");
            if (result.contains("%lp_group%"))        result = result.replace("%lp_group%", "");
            if (result.contains("%lp_prefix%"))       result = result.replace("%lp_prefix%", "");
        }

        if (result.contains("%cross_total%")) {
            int crossTotal = server.getPlayerList().getPlayerCount();
            String localId = CrossServerDb.localServerId(); // hoisted out of the peer loop
            for (PeerSnapshot peer : peers) {
                if (!peer.alive()) continue;
                if (peer.serverId().equals(localId)) continue;
                crossTotal += peer.online();
            }
            result = result.replace("%cross_total%", String.valueOf(crossTotal));
        }

        return result;
    }

    private static List<Component> expandPeers(List<PeerSnapshot> peers) {
        List<Component> out = new java.util.ArrayList<>();
        // When cross-server is disabled (or DB unreachable on a single-server setup),
        // peers is always empty — render nothing rather than the intrusive
        // "no peers reachable" placeholder that wastes a footer line.
        if (peers == null || peers.isEmpty()) {
            return out;
        }

        // Honour the configured peer_order: peers listed there render first in that order,
        // unknown peers keep their DB order at the end. Previously peer_order was defined in
        // config but never consulted, so the footer always used arbitrary DB iteration order.
        List<? extends String> order = TabListConfig.VALUES.peerOrder.get();
        List<PeerSnapshot> ordered;
        if (order != null && !order.isEmpty()) {
            ordered = new java.util.ArrayList<>(peers);
            ordered.sort(java.util.Comparator.comparingInt(p -> {
                int idx = order.indexOf(p.serverId());
                return idx < 0 ? Integer.MAX_VALUE : idx;
            }));
        } else {
            ordered = new java.util.ArrayList<>(peers);
        }

        for (PeerSnapshot p : ordered) {
            String rawName = (p.displayName() != null && !p.displayName().isBlank()) ? p.displayName() : p.serverId();
            // Pad/truncate to a tidy 10-char column so the counts align.
            String name = rawName.length() > 10 ? rawName.substring(0, 10) : String.format("%-10s", rawName);
            String body;
            if (p.alive()) {
                body = String.format("&8• &f%s  &a%d&7/&8%d", name, p.online(), p.max());
            } else {
                body = String.format("&8• &7%s  &coffline", name);
            }
            out.add(translateColors(body));
        }
        return out;
    }

    public static MutableComponent translateColors(String raw) {
        if (raw == null || raw.isEmpty()) return Component.literal("");
        MutableComponent root = Component.literal("");
        StringBuilder buf = new StringBuilder();
        Style style = Style.EMPTY;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(i + 1));
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                if (fmt != null) {
                    if (buf.length() > 0) {
                        root.append(Component.literal(buf.toString()).setStyle(style));
                        buf.setLength(0);
                    }
                    if (fmt == ChatFormatting.RESET) {
                        style = Style.EMPTY;
                    } else if (fmt.isColor()) {
                        style = Style.EMPTY.withColor(fmt);
                    } else {
                        style = style.applyFormat(fmt);
                    }
                    i++;
                    continue;
                }
            }
            buf.append(c);
        }
        if (buf.length() > 0) {
            root.append(Component.literal(buf.toString()).setStyle(style));
        }
        return root;
    }

    private static String formatTps(MinecraftServer server) {
        long[] times = server.getTickTimesNanos();
        if (times == null || times.length == 0) return "20.0";
        double avgMs = 0;
        for (long t : times) avgMs += t / 1_000_000.0;
        avgMs /= times.length;
        double tps = Math.min(20.0, 1000.0 / Math.max(avgMs, 1));
        return String.format("%.1f", tps);
    }

    private static String formatMspt(MinecraftServer server) {
        long[] times = server.getTickTimesNanos();
        if (times == null || times.length == 0) return "0.0";
        double avgMs = 0;
        for (long t : times) avgMs += t / 1_000_000.0;
        avgMs /= times.length;
        return String.format("%.1f", avgMs);
    }

    private static String formatUptime(MinecraftServer server) {
        long seconds = server.getTickCount() / 20L;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private static String formatPlaytime(ServerPlayer player) {
        // Vanilla scoreboard "stat.minecraft.play_one_minute" criterion isn't always reliable;
        // use Stats directly.
        try {
            int ticks = player.getStats().getValue(
                    net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME));
            long seconds = ticks / 20L;
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            if (h > 0) return h + "h " + m + "m";
            return m + "m";
        } catch (Exception e) {
            return "0m";
        }
    }
}
