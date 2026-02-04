package com.vyrriox.arcadiaspawn.events;

import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;
import com.vyrriox.arcadiaspawn.commands.ModCommands;
import com.vyrriox.arcadiaspawn.world.SpawnData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;

import java.util.Set;

@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ServerLevel level = player.serverLevel();

        // Basic check: If player has no played before or data indicates first join
        // For simplicity in this rewrite, we check if they are in the lobby dimension.
        // If not, and we want to force spawn, we can adding logic here.
        // For now, let's ensure that if the spawn is set, they go there on first join
        // (if no other data exists).

        // Note: Minecraft handles player data saving. If a player logs out in the
        // Overworld, they log back in there.
        // We only want to intervene if it's a fresh start or forced.

        // Ideally, we might want a config for "Force Spawn on Join".
        // As per request, optimization is key.
        // Let's implement a simple check: If the player has no 'tags' indicating
        // they've been here, send them to spawn.

        if (!player.getTags().contains("arcadia_visited")) {
            ServerLevel spawnLevel = player.getServer().getLevel(ModCommands.SPAWN_LEVEL_KEY);
            if (spawnLevel != null) {
                SpawnData data = SpawnData.get(spawnLevel);
                if (data.isSet()) {
                    player.resetFallDistance();
                    player.teleportTo(spawnLevel, data.getX(), data.getY(), data.getZ(), Set.of(), data.getYaw(),
                            data.getPitch());
                    player.addTag("arcadia_visited");
                    // Minimal feedback for auto-join, maybe no sound needed?
                    // Let's keep it silent for seamless entry.
                }
            }
        }
    }

    // Optional: Force respawn at lobby if bed is missing
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered())
            return; // Don't interfere with End credits return

        if (event.getEntity() instanceof ServerPlayer player) {
            // Logic: If they have no bed/anchor, they usually go to world spawn.
            // We can override this to be our custom dimension.
            ServerLevel spawnLevel = player.getServer().getLevel(ModCommands.SPAWN_LEVEL_KEY);
            if (spawnLevel != null) {
                SpawnData data = SpawnData.get(spawnLevel);
                // If spawn is set, we can intercept.
                // However, without a clean way to check "did the bed fail?", we might just let
                // Vanilla handle it
                // unless the user specifically requested "All respawns at Lobby".
                // For safety and minimalism, we leave this for now. Directions were "spawn les
                // gens dans cette dimension".
            }
        }
    }

}
