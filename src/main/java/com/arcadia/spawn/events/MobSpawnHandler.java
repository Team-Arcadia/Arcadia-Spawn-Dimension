package com.arcadia.spawn.events;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.config.SpawnConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Filters which {@link MobCategory} categories are allowed to spawn in the custom
 * Arcadia Spawn dimension, driven by {@link SpawnConfig}.
 *
 * <p><b>Server-side only.</b> {@link SpawnConfig} is registered as
 * {@link net.neoforged.fml.config.ModConfig.Type#COMMON} which is <i>not</i>
 * synced from server to client — each side has its own local file. If this
 * handler ran on the {@code ClientLevel} it would read the client's (often
 * default) config and cancel the {@link EntityJoinLevelEvent} client-side,
 * leaving mobs that the server allowed to spawn: server keeps the entity
 * (AI, sounds, hitbox) while the client never adds it to its {@code ClientLevel}
 * → the classic "we hear them but we don't see them" symptom. Short-circuiting
 * on {@code isClientSide()} fixes that — the server's {@code setCanceled} already
 * stops the add-entity packet from being sent, so the client has nothing to filter.
 * Done with an {@code isClientSide} guard (not a {@link net.neoforged.api.distmarker.Dist}
 * filter) so the integrated server in singleplayer still runs it.</p>
 */
@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID)
public class MobSpawnHandler {

    private static final ResourceKey<Level> SPAWN_LEVEL_KEY =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!event.getLevel().dimension().equals(SPAWN_LEVEL_KEY)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        if (!isAllowed(mob.getType().getCategory())) {
            event.setCanceled(true);
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
