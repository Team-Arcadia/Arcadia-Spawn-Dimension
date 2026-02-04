package com.vyrriox.arcadiaspawn.events;

import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;
import com.vyrriox.arcadiaspawn.config.SpawnConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MobSpawnHandler {

    private static final ResourceKey<Level> SPAWN_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        try {
            if (event.getLevel() instanceof Level level && level.dimension().equals(SPAWN_LEVEL_KEY)) {
                if (event.getEntity() instanceof Mob mob) {
                    MobCategory category = mob.getType().getCategory();
                    boolean allowed = isAllowed(category);

                    if (!allowed) {
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Error in MobSpawnHandler: ", e);
        }
    }

    private static boolean isAllowed(MobCategory category) {
        return switch (category) {
            case MONSTER -> SpawnConfig.COMMON.spawnMonsters.get();
            case CREATURE -> SpawnConfig.COMMON.spawnCreatures.get();
            case AMBIENT -> SpawnConfig.COMMON.spawnAmbient.get();
            case WATER_CREATURE -> SpawnConfig.COMMON.spawnWaterCreatures.get();
            case WATER_AMBIENT -> SpawnConfig.COMMON.spawnWaterAmbient.get();
            case UNDERGROUND_WATER_CREATURE -> SpawnConfig.COMMON.spawnUndergroundWaterCreatures.get();
            case AXOLOTLS -> SpawnConfig.COMMON.spawnAxolotls.get();
            case MISC -> SpawnConfig.COMMON.spawnMisc.get();
            default -> false;
        };
    }
}
