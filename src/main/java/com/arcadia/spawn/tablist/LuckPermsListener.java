package com.arcadia.spawn.tablist;

import com.arcadia.spawn.ArcadiaSpawnMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Subscribes to LuckPerms UserDataRecalculateEvent so that promoting/demoting a
 * player triggers an immediate tab list team re-sync (instead of waiting for the
 * next periodic refresh).
 *
 * Isolated in its own class — the JVM will only load LP classes when
 * {@link #tryAttach()} is actually called, which itself is only invoked when
 * {@link GradeResolver#hasLuckPerms()} returns true.
 */
public final class LuckPermsListener {

    private static volatile boolean attached = false;

    private LuckPermsListener() {}

    public static synchronized void tryAttach() {
        if (attached) return;
        if (!GradeResolver.hasLuckPerms()) return;
        try {
            var lp = net.luckperms.api.LuckPermsProvider.get();
            lp.getEventBus().subscribe(
                    net.luckperms.api.event.user.UserDataRecalculateEvent.class,
                    event -> {
                        try {
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server == null) return;
                            ServerPlayer player = server.getPlayerList().getPlayer(event.getUser().getUniqueId());
                            if (player != null) {
                                server.execute(() -> TabListManager.onGradeChanged(player));
                            }
                        } catch (Exception ignored) {}
                    }
            );
            attached = true;
            ArcadiaSpawnMod.LOGGER.info("LuckPerms UserDataRecalculateEvent listener attached.");
        } catch (Throwable t) {
            ArcadiaSpawnMod.LOGGER.warn("Could not attach LuckPerms listener: {}", t.getMessage());
        }
    }
}
