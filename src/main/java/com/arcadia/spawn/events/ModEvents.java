package com.arcadia.spawn.events;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.commands.SpawnCommands;
import com.arcadia.spawn.commands.DebugCommands;
import com.arcadia.spawn.commands.TeleportHelper;
import com.arcadia.spawn.config.SpawnConfig;
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
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TeleportHelper.tick();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportHelper.onDisconnect(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            // If player has a valid bed/anchor and force_respawn is off, skip
            if (player.getRespawnPosition() != null && !SpawnConfig.COMMON.forceSpawnOnRespawn.get()) return;

            // If player has no bed/anchor, OR force_respawn is enabled, send to spawn
            SpawnData data = SpawnData.get();
            if (data.isSet()) {
                ServerLevel targetLevel = player.getServer().getLevel(data.getDimensionKey());
                if (targetLevel != null) {
                    // Schedule for next tick to ensure respawn completes first
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
