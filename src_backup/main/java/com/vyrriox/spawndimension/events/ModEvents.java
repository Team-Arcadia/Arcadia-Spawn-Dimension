package com.vyrriox.spawndimension.events;

import com.vyrriox.spawndimension.SpawnDimensionMod;
import com.vyrriox.spawndimension.commands.ModCommands;
import com.vyrriox.spawndimension.world.ModDimensions;
import com.vyrriox.spawndimension.world.SpawnData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;

// @EventBusSubscriber(modid = SpawnDimensionMod.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            if (!data.contains("vp_first_join")) {
                data.putBoolean("vp_first_join", true);

                // Teleport to lobby spawn
                ServerLevel spawnLevel = player.getServer().getLevel(ModDimensions.SPAWN_LEVEL_KEY);
                if (spawnLevel != null) {
                    SpawnData spawnData = SpawnData.get(spawnLevel);
                    if (spawnData.isSet()) {
                        player.teleportTo(spawnLevel, spawnData.getX(), spawnData.getY(), spawnData.getZ(), Set.of(),
                                spawnData.getYaw(), spawnData.getPitch());
                        player.sendSystemMessage(Component.literal("Bienvenue sur le serveur !")); // French as
                                                                                                   // requested per user
                                                                                                   // rules (implicitly
                                                                                                   // or explicitly if
                                                                                                   // "global rules"
                                                                                                   // apply, user said
                                                                                                   // "Français
                                                                                                   // exclusif" in
                                                                                                   // system prompt
                                                                                                   // rules)
                    }
                }
            }
        }
    }
}
