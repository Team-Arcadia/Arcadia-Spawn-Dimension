package com.arcadia.spawn.tablist;

import com.arcadia.spawn.ArcadiaSpawnMod;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
 *   - When a player becomes spectator: send PlayerInfoRemovePacket to every non-spec player.
 *   - When a player leaves spectator: re-send PlayerInfoUpdatePacket (ADD_PLAYER + game mode +
 *     latency + display name) to every non-spec player so they re-appear.
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
        var removePacket = new ClientboundPlayerInfoRemovePacket(List.of(spectator.getUUID()));
        for (ServerPlayer viewer : all) {
            // Skip the spectator themselves and other spectators.
            if (viewer.getUUID().equals(spectator.getUUID())) continue;
            if (viewer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
            try {
                viewer.connection.send(removePacket);
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.debug("Failed to send hide packet to {}: {}",
                        viewer.getName().getString(), e.getMessage());
            }
        }
    }

    private static void revealToAll(ServerPlayer player, List<ServerPlayer> all) {
        // ADD_PLAYER + INITIALIZE_CHAT + GAME_MODE + LATENCY + LISTED + DISPLAY_NAME
        // covers everything the vanilla client needs to redraw the row.
        EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
        var addPacket = new ClientboundPlayerInfoUpdatePacket(actions, Collections.singletonList(player));

        for (ServerPlayer viewer : all) {
            if (viewer.getUUID().equals(player.getUUID())) continue;
            // Spectators were already seeing this player — no harm in re-sending but skip to save bandwidth.
            if (viewer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) continue;
            try {
                viewer.connection.send(addPacket);
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.debug("Failed to send reveal packet to {}: {}",
                        viewer.getName().getString(), e.getMessage());
            }
        }
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
