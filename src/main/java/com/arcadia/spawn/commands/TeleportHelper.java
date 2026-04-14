package com.arcadia.spawn.commands;

import com.arcadia.lib.teleport.TeleportManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

/**
 * Wrapper around TeleportManager that fires NeoForge EntityTeleportEvent
 * before teleporting. This ensures FTB Essentials /back and other mods
 * that listen for teleport events can record the pre-teleport position.
 */
public final class TeleportHelper {

    private TeleportHelper() {}

    /**
     * Teleports a player immediately, firing EntityTeleportEvent first.
     * If any listener cancels the event, the teleport is aborted.
     */
    public static boolean teleportNow(ServerPlayer player, Vec3 target, ServerLevel level) {
        EntityTeleportEvent.TeleportCommand event = new EntityTeleportEvent.TeleportCommand(
                player, target.x, target.y, target.z);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return false;
        }

        Vec3 adjusted = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        TeleportManager.teleportNow(player, adjusted, level);
        return true;
    }

    /**
     * Teleports a player with warmup, firing EntityTeleportEvent when warmup completes.
     * Note: The event is fired immediately to record /back position, then warmup starts.
     * If warmup is 0, teleports instantly.
     */
    public static void teleportWithWarmup(ServerPlayer player, Vec3 target, ServerLevel level,
                                          int warmupTicks, int cooldownMs, String actionId) {
        // Fire event immediately so /back position is recorded
        EntityTeleportEvent.TeleportCommand event = new EntityTeleportEvent.TeleportCommand(
                player, target.x, target.y, target.z);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return;
        }

        Vec3 adjusted = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());

        if (warmupTicks > 0) {
            TeleportManager.teleportWithWarmup(player, adjusted, level, warmupTicks, cooldownMs, actionId);
        } else {
            TeleportManager.teleportNow(player, adjusted, level);
        }
    }
}
