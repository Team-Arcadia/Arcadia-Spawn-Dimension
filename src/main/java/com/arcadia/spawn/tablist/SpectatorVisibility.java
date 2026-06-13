package com.arcadia.spawn.tablist;

import com.arcadia.spawn.ArcadiaSpawnMod;
import net.minecraft.Optionull;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hides spectator-mode players from the tab list of non-spectators.
 *
 * Rules:
 *   - Spectators are hidden from the tab list of players in SURVIVAL / CREATIVE / ADVENTURE.
 *   - Spectators remain visible to other spectators (so co-mod can coordinate).
 *   - A spectator always sees themselves in their own tab (Minecraft enforces this).
 *
 * Mechanism:
 *   - Toggle the per-player "listed" flag via {@link ClientboundPlayerInfoUpdatePacket}
 *     {@code UPDATE_LISTED}. listed=false drops the tab row; listed=true restores it.
 *   - This NEVER removes the client-side PlayerInfo, so the player entity is never
 *     destroyed. A previous implementation used PlayerInfoRemovePacket, which deleted the
 *     client PlayerInfo — the vanilla client then refused to re-spawn the player entity
 *     (createEntityFromPacket null-checks the PlayerInfo), leaving staff invisible after
 *     they left spectator until a full re-track happened. UPDATE_LISTED avoids that path.
 *
 * Lifecycle hooks come from ModEvents (game-mode change event + refresh-tick fallback).
 *
 * Opt-in via TabListConfig.hideSpectatorsFromTab.
 */
public final class SpectatorVisibility {

    private SpectatorVisibility() {}

    /** UUIDs that we've actively hidden so we can detect "back to non-spec" transitions. */
    private static final Set<UUID> HIDDEN = ConcurrentHashMap.newKeySet();

    /**
     * Reconcile the visibility state with the current game modes. Called from the
     * tab-list refresh tick — cheap when nothing changed (no packets sent).
     */
    public static void reconcile(MinecraftServer server) {
        if (!TabListConfig.VALUES.hideSpectatorsFromTab.get()) {
            // Feature disabled: if we have anything hidden, un-hide it.
            if (!HIDDEN.isEmpty()) clearAll(server);
            return;
        }
        if (server == null) return;

        List<ServerPlayer> all = server.getPlayerList().getPlayers();
        Set<UUID> currentSpectators = new HashSet<>();
        for (ServerPlayer p : all) {
            if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                currentSpectators.add(p.getUUID());
            }
        }

        // 1. Hide newly-spectator players from non-spec.
        for (UUID specUuid : currentSpectators) {
            if (HIDDEN.add(specUuid)) {
                ServerPlayer specPlayer = server.getPlayerList().getPlayer(specUuid);
                if (specPlayer != null) hideFromNonSpec(specPlayer, all);
            }
        }

        // 2. Reveal players that were spec but aren't anymore (or disconnected).
        List<UUID> toReveal = new ArrayList<>();
        for (UUID uuid : HIDDEN) {
            if (!currentSpectators.contains(uuid)) toReveal.add(uuid);
        }
        for (UUID uuid : toReveal) {
            HIDDEN.remove(uuid);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) revealToAll(player, all);
        }
    }

    /** Called when a player explicitly changes game mode (event-driven path). */
    public static void onGameModeChange(ServerPlayer player, GameType newMode) {
        if (!TabListConfig.VALUES.hideSpectatorsFromTab.get()) return;
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        List<ServerPlayer> all = server.getPlayerList().getPlayers();
        if (newMode == GameType.SPECTATOR) {
            if (HIDDEN.add(player.getUUID())) hideFromNonSpec(player, all);
        } else {
            if (HIDDEN.remove(player.getUUID())) revealToAll(player, all);
        }
    }

    /** Called on player disconnect so we don't leak state. */
    public static void onPlayerDisconnect(UUID uuid) {
        HIDDEN.remove(uuid);
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private static void hideFromNonSpec(ServerPlayer spectator, List<ServerPlayer> all) {
        broadcastListed(spectator, all, false);
    }

    private static void revealToAll(ServerPlayer player, List<ServerPlayer> all) {
        broadcastListed(player, all, true);
    }

    /**
     * Send an {@code UPDATE_LISTED} packet for {@code target} to every non-spectator viewer,
     * toggling whether the target shows up in their tab list.
     *
     * <p>listed=false removes the tab row only; the client keeps the PlayerInfo and the player
     * entity, so it stays rendered and re-tracks correctly. listed=true restores the row.
     */
    private static void broadcastListed(ServerPlayer target, List<ServerPlayer> all, boolean listed) {
        ClientboundPlayerInfoUpdatePacket packet = listedPacket(target, listed);
        for (ServerPlayer viewer : all) {
            // Skip the target themselves and other spectators (spectators always see everyone).
            if (viewer.getUUID().equals(target.getUUID())) continue;
            if (viewer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
            try {
                viewer.connection.send(packet);
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.debug("Failed to send listed={} packet to {}: {}",
                        listed, viewer.getName().getString(), e.getMessage());
            }
        }
    }

    /**
     * Build a single-player {@code UPDATE_LISTED} packet. The vanilla public constructors
     * hard-code listed=true in {@code Entry(ServerPlayer)}, so we build the packet with the
     * convenience ctor (which correctly sets {@code actions = {UPDATE_LISTED}}) and then swap
     * in an {@code Entry} carrying the desired {@code listed} flag. The {@code entries} field
     * is made writable via the access transformer.
     */
    private static ClientboundPlayerInfoUpdatePacket listedPacket(ServerPlayer player, boolean listed) {
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, player);
        ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                player.getUUID(),
                player.getGameProfile(),
                listed,
                player.connection.latency(),
                player.gameMode.getGameModeForPlayer(),
                player.getTabListDisplayName(),
                Optionull.map(player.getChatSession(), RemoteChatSession::asData));
        packet.entries = List.of(entry);
        return packet;
    }

    private static void clearAll(MinecraftServer server) {
        List<ServerPlayer> all = server.getPlayerList().getPlayers();
        for (UUID uuid : new ArrayList<>(HIDDEN)) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) revealToAll(p, all);
        }
        HIDDEN.clear();
    }
}
