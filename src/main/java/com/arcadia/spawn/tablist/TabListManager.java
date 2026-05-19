package com.arcadia.spawn.tablist;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.tablist.CrossServerDb.PeerSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central manager: applies header/footer to every online player on a refresh
 * tick, syncs sortable PlayerTeam entries from grade resolution, and keeps an
 * async snapshot of cross-server peers up to date.
 *
 * All work is event-driven — no dedicated thread, no scheduled executor. The
 * server tick event is the only beat.
 */
public final class TabListManager {

    private TabListManager() {}

    private static final AtomicReference<List<PeerSnapshot>> PEER_CACHE =
            new AtomicReference<>(Collections.emptyList());
    private static long lastHeartbeatMs = 0L;
    private static long lastPeerFetchMs = 0L;
    private static int tickCounter = 0;

    /** Called from ServerTickEvent.Post. Cheap when disabled. */
    public static void tick(MinecraftServer server) {
        if (!TabListConfig.VALUES.enabled.get()) return;
        if (server == null) return;

        int interval = TabListConfig.VALUES.refreshIntervalTicks.get();
        if (++tickCounter < interval) return;
        tickCounter = 0;

        // Advance animation frames once per refresh tick — animations cycle through
        // their frame lists at exactly this rate.
        AnimationFrames.advance();

        // Cross-server: heartbeat + peer refresh on their own cadences
        if (TabListConfig.VALUES.crossServerEnabled.get()) {
            long now = System.currentTimeMillis();

            long hbMs = TabListConfig.VALUES.heartbeatIntervalSeconds.get() * 1000L;
            if (now - lastHeartbeatMs >= hbMs) {
                lastHeartbeatMs = now;
                CrossServerDb.heartbeat(
                        resolveServerDisplayName(),
                        server.getPlayerList().getPlayerCount(),
                        server.getMaxPlayers()
                );
            }

            // refresh peers a little more aggressively than heartbeat so footer is fresh
            long peerMs = Math.max(2000L, hbMs / 2);
            if (now - lastPeerFetchMs >= peerMs) {
                lastPeerFetchMs = now;
                long timeoutMs = TabListConfig.VALUES.peerTimeoutSeconds.get() * 1000L;
                CompletableFuture<List<PeerSnapshot>> fut = CrossServerDb.fetchPeers(timeoutMs);
                fut.thenAccept(list -> PEER_CACHE.set(list == null ? Collections.emptyList() : list));
            }
        } else {
            PEER_CACHE.set(Collections.emptyList());
        }

        // Reconcile spectator visibility before sending header/footer so any pending
        // game-mode change we missed (e.g. /gamemode spectator from console) is honored.
        SpectatorVisibility.reconcile(server);

        List<PeerSnapshot> peers = PEER_CACHE.get();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            apply(player, server, peers);
        }
    }

    /** Called on PlayerLoggedInEvent. Immediately syncs the team + header/footer. */
    public static void onPlayerJoin(ServerPlayer player) {
        if (!TabListConfig.VALUES.enabled.get()) return;
        if (player == null) return;

        syncTeamFor(player);
        apply(player, player.getServer(), PEER_CACHE.get());
    }

    /** Called on UserDataRecalculateEvent (LuckPerms) — refreshes the team for one player. */
    public static void onGradeChanged(ServerPlayer player) {
        if (!TabListConfig.VALUES.enabled.get()) return;
        if (player == null) return;
        syncTeamFor(player);
    }

    /** Called on server shutdown. Removes this server's row from the DB. */
    public static void onShutdown() {
        if (!TabListConfig.VALUES.crossServerEnabled.get()) return;
        CrossServerDb.cleanup();
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private static void apply(ServerPlayer player, MinecraftServer server, List<PeerSnapshot> peers) {
        String displayName = resolveServerDisplayName();

        Component header = PlaceholderFormatter.formatLines(
                TabListConfig.VALUES.headerLines.get(), player, server, displayName, peers);
        Component footer = PlaceholderFormatter.formatLines(
                TabListConfig.VALUES.footerLines.get(), player, server, displayName, peers);

        try {
            player.connection.send(new ClientboundTabListPacket(header, footer));
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.debug("tab list send failed for {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    private static void syncTeamFor(ServerPlayer player) {
        if (!TabListConfig.VALUES.groupSortingEnabled.get()) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        Scoreboard scoreboard = server.getScoreboard();

        GradeResolver.Grade grade = GradeResolver.resolve(player);

        // Team name: as_<999 - clamp(weight, 0, 999)>_<gradeId>
        // -> highest weight = smallest prefix = sorts first alphabetically
        int w = Math.max(0, Math.min(999, grade.weight()));
        String teamName = String.format("as_%03d_%s", 999 - w, safe(grade.id()));
        teamName = teamName.length() > 16 ? teamName.substring(0, 16) : teamName;

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setCollisionRule(Team.CollisionRule.NEVER);
            team.setSeeFriendlyInvisibles(false);
        }

        // Prefix from LuckPerms meta (translated) when enabled.
        // Ensure a trailing space so the prefix is visually separated from the player
        // name (LP sometimes returns a prefix without a trailing space which makes
        // "[Owner]Player" stick together in the tab list).
        if (TabListConfig.VALUES.showLuckPermsPrefix.get() && grade.prefix() != null && !grade.prefix().isEmpty()) {
            String raw = grade.prefix();
            if (!raw.endsWith(" ") && !raw.endsWith("&r ") && !raw.endsWith("§r ")) {
                raw = raw + " ";
            }
            team.setPlayerPrefix(PlaceholderFormatter.translateColors(raw));
        } else {
            team.setPlayerPrefix(Component.literal(""));
        }
        team.setPlayerSuffix(Component.literal(""));

        // Optional name color from LP meta
        if (grade.color() != -1) {
            ChatFormatting nearest = nearestChatFormat(grade.color());
            if (nearest != null) team.setColor(nearest);
        }

        String entry = player.getGameProfile().getName();
        // Remove from any other arcadia-spawn team first (entries are unique per scoreboard)
        for (PlayerTeam existing : scoreboard.getPlayerTeams()) {
            if (existing == team) continue;
            if (!existing.getName().startsWith("as_")) continue;
            if (existing.getPlayers().contains(entry)) {
                scoreboard.removePlayerFromTeam(entry, existing);
            }
        }
        scoreboard.addPlayerToTeam(entry, team);
    }

    private static String resolveServerDisplayName() {
        String configured = TabListConfig.VALUES.serverDisplayName.get();
        if (configured != null && !configured.isBlank()) return configured;
        return CrossServerDb.localServerId();
    }

    private static String safe(String s) {
        if (s == null) return "x";
        String out = s.toLowerCase().replaceAll("[^a-z0-9_]", "");
        return out.isEmpty() ? "x" : out;
    }

    private static ChatFormatting nearestChatFormat(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        ChatFormatting best = ChatFormatting.WHITE;
        int bestDist = Integer.MAX_VALUE;
        for (ChatFormatting fmt : ChatFormatting.values()) {
            if (!fmt.isColor()) continue;
            Integer rgb = fmt.getColor();
            if (rgb == null) continue;
            int fr = (rgb >> 16) & 0xFF, fg = (rgb >> 8) & 0xFF, fb = rgb & 0xFF;
            int d = (r - fr) * (r - fr) + (g - fg) * (g - fg) + (b - fb) * (b - fb);
            if (d < bestDist) {
                bestDist = d;
                best = fmt;
            }
        }
        return best;
    }
}
