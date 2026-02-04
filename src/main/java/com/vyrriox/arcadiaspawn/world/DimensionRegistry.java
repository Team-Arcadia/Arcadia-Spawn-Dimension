package com.vyrriox.arcadiaspawn.world;

import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;
import com.vyrriox.arcadiaspawn.config.SpawnConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DimensionRegistry {

    public static final ResourceKey<DimensionType> SPAWN_DIM_TYPE_KEY = ResourceKey.create(Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));
    public static final ResourceKey<LevelStem> SPAWN_LEVEL_STEM_KEY = ResourceKey.create(Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.DIMENSION_TYPE, helper -> {
            helper.register(SPAWN_DIM_TYPE_KEY, createDimensionType());
        });

        event.register(Registries.LEVEL_STEM, helper -> {
            ResourceLocation biomeId;
            try {
                biomeId = ResourceLocation.parse(SpawnConfig.COMMON.biome.get());
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.error("Invalid biome config: '{}'. Using default.",
                        SpawnConfig.COMMON.biome.get(), e);
                biomeId = ResourceLocation.fromNamespaceAndPath("minecraft", "the_void");
            }

            Registry<Biome> biomeRegistry = (Registry<Biome>) BuiltInRegistries.REGISTRY
                    .get(Registries.BIOME.location());
            if (biomeRegistry == null) {
                throw new RuntimeException("Biome Registry not found!");
            }

            ResourceLocation finalBiomeId = biomeId;
            Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, biomeId))
                    .orElseGet(() -> {
                        ArcadiaSpawnMod.LOGGER.error("Biome '{}' not found in registry. Using default.", finalBiomeId);
                        return biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
                    });

            List<FlatLayerInfo> layers = parseLayersSafe(SpawnConfig.COMMON.flatLayers.get());
            if (layers.isEmpty()) {
                ArcadiaSpawnMod.LOGGER.warn("No valid layers found in config. Using default bedrock layer.");
                layers.add(new FlatLayerInfo(1, net.minecraft.world.level.block.Blocks.BEDROCK));
            }

            Optional<HolderSet<StructureSet>> structureOverrides = Optional.empty();

            Holder<PlacedFeature> emptyFeature = Holder.direct(new PlacedFeature(
                    Holder.direct(new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.INSTANCE)),
                    List.of()));

            // Use reflection helper to bypass visibility
            FlatLevelGeneratorSettings settings = createSettings(
                    structureOverrides,
                    layers,
                    false,
                    SpawnConfig.COMMON.latesAndFeatures.get(),
                    Optional.of(biomeHolder),
                    biomeHolder,
                    emptyFeature,
                    emptyFeature);

            FlatLevelSource source = new FlatLevelSource(settings);

            LevelStem stem = new LevelStem(
                    Holder.direct(createDimensionType()),
                    source);

            helper.register(SPAWN_LEVEL_STEM_KEY, stem);
        });
    }

    private static List<FlatLayerInfo> parseLayersSafe(List<? extends String> layerStrings) {
        List<FlatLayerInfo> layers = new ArrayList<>();
        for (String s : layerStrings) {
            try {
                String[] split = s.split("\\*");
                int count = 1;
                String blockId;
                if (split.length == 2) {
                    count = Integer.parseInt(split[0]);
                    blockId = split[1];
                } else {
                    blockId = split[0];
                }

                ResourceLocation loc = ResourceLocation.tryParse(blockId);
                if (loc == null) {
                    ArcadiaSpawnMod.LOGGER.error("Invalid block ID format in layer config: {}", blockId);
                    continue;
                }

                Block block = BuiltInRegistries.BLOCK.get(loc);
                boolean isAir = block == net.minecraft.world.level.block.Blocks.AIR;
                boolean meantAir = "minecraft:air".equals(blockId) || "air".equals(blockId);

                if (isAir && !meantAir) {
                    ArcadiaSpawnMod.LOGGER.error("Block '{}' not found (resolved to AIR). Skipping layer.", blockId);
                    continue;
                }

                layers.add(new FlatLayerInfo(count, block));
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.error("Failed to parse layer config entry: {}", s, e);
            }
        }
        return layers;
    }

    private static DimensionType createDimensionType() {
        return new DimensionType(
                SpawnConfig.COMMON.fixedTime.get().equals(OptionalLong.empty()) ? OptionalLong.empty()
                        : (SpawnConfig.COMMON.timeLocked.get() ? OptionalLong.of(SpawnConfig.COMMON.fixedTime.get())
                                : OptionalLong.empty()),
                SpawnConfig.COMMON.hasSkylight.get(),
                SpawnConfig.COMMON.hasCeiling.get(),
                SpawnConfig.COMMON.ultrawarm.get(),
                SpawnConfig.COMMON.natural.get(),
                SpawnConfig.COMMON.coordinateScale.get(),
                SpawnConfig.COMMON.bedWorks.get(),
                SpawnConfig.COMMON.respawnAnchorWorks.get(),
                SpawnConfig.COMMON.minY.get(),
                SpawnConfig.COMMON.height.get(),
                SpawnConfig.COMMON.logicalHeight.get(),
                TagKey.create(Registries.BLOCK, ResourceLocation.parse(SpawnConfig.COMMON.infiniburn.get())),
                ResourceLocation.parse(SpawnConfig.COMMON.effects.get()),
                SpawnConfig.COMMON.ambientLight.get().floatValue(),
                new DimensionType.MonsterSettings(
                        SpawnConfig.COMMON.piglinSafe.get(),
                        SpawnConfig.COMMON.hasRaids.get(),
                        Const(SpawnConfig.COMMON.monsterSpawnLightLevel.get()),
                        SpawnConfig.COMMON.monsterSpawnBlockLightLimit.get()));
    }

    private static ConstantInt Const(int value) {
        return ConstantInt.of(value);
    }

    // Helper to bypass private constructor visibility using reflection
    private static FlatLevelGeneratorSettings createSettings(
            Optional<HolderSet<StructureSet>> structureOverrides,
            List<FlatLayerInfo> layers,
            boolean addLakes,
            boolean addFeatures,
            Optional<Holder<Biome>> biome,
            Holder.Reference<Biome> biomeFallback,
            Holder<PlacedFeature> lakeFeature,
            Holder<PlacedFeature> lavaLakeFeature) {
        try {
            java.lang.reflect.Constructor<FlatLevelGeneratorSettings> ctor = FlatLevelGeneratorSettings.class
                    .getDeclaredConstructor(
                            Optional.class, List.class, boolean.class, boolean.class, Optional.class, Holder.class,
                            Holder.class, Holder.class);
            ctor.setAccessible(true);
            return ctor.newInstance(structureOverrides, layers, addLakes, addFeatures, biome, biomeFallback,
                    lakeFeature, lavaLakeFeature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create FlatLevelGeneratorSettings via reflection", e);
        }
    }
}
