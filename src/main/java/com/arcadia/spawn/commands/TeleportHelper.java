package com.arcadia.spawn.commands;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.lib.player.CooldownManager;
import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.lobby.LocalizationManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bilingual teleport system with warmup, cooldown, movement cancellation,
 * FTB /back compatibility, and LuckPerms meta override support.
 */
public final class TeleportHelper {

    private static final Map<UUID, WarmupTask> activeWarmups = new ConcurrentHashMap<>();

    private TeleportHelper() {}

    // ── Instant teleport ────────────────────────────────────────────────────

    public static boolean teleportNow(ServerPlayer player, Vec3 target, ServerLevel level) {
        EntityTeleportEvent.TeleportCommand event = new EntityTeleportEvent.TeleportCommand(
                player, target.x, target.y, target.z);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) return false;

        Vec3 adjusted = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        player.teleportTo(level, adjusted.x, adjusted.y, adjusted.z,
                java.util.Set.of(), player.getYRot(), player.getXRot());
        level.playSound(null, BlockPos.containing(adjusted),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.0f);
        return true;
    }

    // ── Warmup teleport (bilingual messages) ────────────────────────────────

    public static void teleportWithWarmup(ServerPlayer player, Vec3 target, ServerLevel level,
                                          int warmupTicks, int cooldownMs, String actionId) {
        UUID uuid = player.getUUID();

        // Check cooldown
        if (!CooldownManager.isReady(uuid, actionId)) {
            String remaining = CooldownManager.getRemainingFormatted(uuid, actionId);
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.tp.cooldown", remaining)));
            return;
        }

        // Fire FTB /back event
        EntityTeleportEvent.TeleportCommand event = new EntityTeleportEvent.TeleportCommand(
                player, target.x, target.y, target.z);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) return;

        Vec3 adjusted = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());

        // Apply LuckPerms meta overrides
        int actualWarmup = getMetaOverride(player, actionId + ".warmup", warmupTicks);
        int actualCooldown = getMetaOverride(player, actionId + ".cooldown", cooldownMs);

        if (actualWarmup <= 0) {
            // Instant teleport
            executeTP(player, adjusted, level);
            if (actualCooldown > 0) {
                CooldownManager.set(uuid, actionId, actualCooldown);
            }
            return;
        }

        // Cancel existing warmup
        cancelWarmup(uuid);

        // Start warmup
        activeWarmups.put(uuid, new WarmupTask(adjusted, level, actualWarmup, actualCooldown,
                actionId, player.position(), System.currentTimeMillis()));

        int seconds = Math.max(1, actualWarmup / 20);
        player.sendSystemMessage(ArcadiaMessages.info(
                LocalizationManager.getString(player, "arcadia_spawn.tp.warmup", seconds)));
    }

    // ── Tick (call from server tick event) ───────────────────────────────────

    public static void tick() {
        var it = activeWarmups.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID uuid = entry.getKey();
            WarmupTask task = entry.getValue();

            ServerPlayer player = findPlayer(uuid);
            if (player == null) { it.remove(); continue; }

            // Movement check (> 0.3 blocks)
            if (player.position().distanceTo(task.startPos) > 0.3) {
                player.sendSystemMessage(ArcadiaMessages.error(
                        LocalizationManager.getString(player, "arcadia_spawn.tp.cancelled")));
                it.remove();
                continue;
            }

            // Check if warmup complete
            long elapsed = System.currentTimeMillis() - task.startTime;
            if (elapsed >= task.warmupTicks * 50L) {
                executeTP(player, task.target, task.level);

                if (task.cooldownMs > 0) {
                    CooldownManager.set(uuid, task.actionId, task.cooldownMs);
                }
                it.remove();
            }
        }
    }

    public static void cancelWarmup(UUID uuid) { activeWarmups.remove(uuid); }
    public static boolean hasWarmup(UUID uuid) { return activeWarmups.containsKey(uuid); }
    public static void onDisconnect(UUID uuid) { activeWarmups.remove(uuid); }

    // ── LuckPerms meta override ─────────────────────────────────────────────

    /**
     * Reads a LuckPerms meta value for a player.
     * Meta key format: "arcadia_spawn.<key>" (e.g. "arcadia_spawn.spawn_tp.warmup")
     * Returns defaultValue if meta not found or LuckPerms not present.
     */
    private static int getMetaOverride(ServerPlayer player, String key, int defaultValue) {
        try {
            net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUUID());
            if (user == null) return defaultValue;

            String metaKey = "arcadia_spawn." + key;
            String value = user.getCachedData().getMetaData().getMetaValue(metaKey);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (Exception ignored) {
            // LuckPerms not available or meta not set
        }
        return defaultValue;
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private static void executeTP(ServerPlayer player, Vec3 target, ServerLevel level) {
        player.teleportTo(level, target.x, target.y, target.z,
                java.util.Set.of(), player.getYRot(), player.getXRot());
        level.playSound(null, BlockPos.containing(target),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private static ServerPlayer findPlayer(UUID uuid) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    private record WarmupTask(Vec3 target, ServerLevel level, int warmupTicks,
                              int cooldownMs, String actionId, Vec3 startPos, long startTime) {}
}
