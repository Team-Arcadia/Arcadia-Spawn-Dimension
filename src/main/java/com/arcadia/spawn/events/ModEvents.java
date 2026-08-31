package com.arcadia.spawn.events;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.commands.SpawnCommands;
import com.arcadia.spawn.commands.DebugCommands;
import com.arcadia.spawn.commands.TeleportHelper;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.tablist.LuckPermsListener;
import com.arcadia.spawn.tablist.SpectatorVisibility;
import com.arcadia.spawn.tablist.TabListManager;
import com.arcadia.spawn.util.RateLimiter;
import com.arcadia.spawn.world.CustomDimensionPack;
import com.arcadia.spawn.world.SpawnData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID)
public class ModEvents {

    public static final ResourceKey<Level> SPAWN_LEVEL_KEY =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SpawnCommands.register(event.getDispatcher(), event.getBuildContext());
        DebugCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SpawnConfig.COMMON.forceSpawnOnFirstJoin.get()) return;

        if (!player.getTags().contains("arcadia_visited")) {
            SpawnData data = SpawnData.get();
            if (data.isSet()) {
                ServerLevel targetLevel = player.getServer().getLevel(data.getDimensionKey());
                if (targetLevel != null) {
                    player.resetFallDistance();
                    player.teleportTo(targetLevel, data.getX(), data.getY(), data.getZ(),
                            Set.of(), data.getYaw(), data.getPitch());
                    player.addTag("arcadia_visited");
                    ArcadiaSpawnMod.LOGGER.debug("First join: sent {} to {}.", player.getName().getString(), data.getDimensionId());
                }
            }
        }

        // TabList: sync team + send header/footer immediately so the player
        // doesn't see vanilla tab for a tick before the refresh.
        TabListManager.onPlayerJoin(player);

        // Reconcile spectator visibility on join: covers a player relogging while in
        // spectator (must be hidden) and a player relogging out of spectator while still
        // flagged hidden from a previous session (must be revealed). The next refresh tick
        // would catch it eventually, but doing it here avoids a visible delay.
        SpectatorVisibility.reconcile(player.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TeleportHelper.tick();
        TabListManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportHelper.onDisconnect(player.getUUID());
            RateLimiter.onDisconnect(player.getUUID());
            SpectatorVisibility.onPlayerDisconnect(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpectatorVisibility.onGameModeChange(player, event.getNewGameMode());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Hot-attach the LuckPerms listener once the server is up so we react
        // to /lp parent set, /lp group <g> setweight, etc. Safe no-op without LP.
        LuckPermsListener.tryAttach();
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Runs before the levels are created, so a dimension folder deleted here cannot be
        // reopened or recreated by a running ServerLevel. This is also the retry point for
        // a purge the previous run was killed before finishing.
        CustomDimensionPack.runPendingPurges(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TabListManager.onShutdown();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // Every level is closed and the save is unlocked by now, so the files are free.
        // Doing it here means "delete + stop the server" leaves the save already clean.
        CustomDimensionPack.runPendingPurges(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getRespawnPosition() != null && !SpawnConfig.COMMON.forceSpawnOnRespawn.get()) return;

            SpawnData data = SpawnData.get();
            if (data.isSet()) {
                ServerLevel targetLevel = player.getServer().getLevel(data.getDimensionKey());
                if (targetLevel != null) {
                    player.getServer().execute(() -> {
                        player.resetFallDistance();
                        player.teleportTo(targetLevel, data.getX(), data.getY(), data.getZ(),
                                Set.of(), data.getYaw(), data.getPitch());
                        ArcadiaSpawnMod.LOGGER.debug("Respawn: sent {} to spawn at {}.",
                                player.getName().getString(), data.getDimensionId());
                    });
                }
            }
        }
    }
}
