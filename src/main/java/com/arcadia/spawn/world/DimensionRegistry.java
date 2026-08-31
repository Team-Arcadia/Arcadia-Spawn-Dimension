package com.arcadia.spawn.world;

import com.arcadia.spawn.ArcadiaSpawnMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Keys and shared world-shape helpers for the dimensions this mod owns.
 *
 * The built-in {@code arcadia:spawn} dimension is defined by the data pack shipped in
 * the jar (data/arcadia/dimension/spawn.json + dimension_type/spawn.json). Custom
 * dimensions are generated into a data pack by {@link CustomDimensionPack}.
 *
 * Nothing here registers anything through {@code RegisterEvent}: dimension types and
 * level stems live in data pack registries, which are rebuilt from packs at every world
 * load, and {@code RegisterEvent} only fires for the static registries listed in
 * {@code BuiltInRegistries}. A {@code helper.register()} call for those keys is silently
 * dropped, which is what kept custom dimensions from ever loading before 1.5.7.
 */
public final class DimensionRegistry {

    public static final ResourceKey<DimensionType> SPAWN_DIM_TYPE_KEY =
            ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));
    public static final ResourceKey<LevelStem> SPAWN_LEVEL_STEM_KEY =
            ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath("arcadia", "spawn"));

    private static final int MAX_TOTAL_HEIGHT = 2032;

    private DimensionRegistry() {}

    /**
     * Clamps min_y to Minecraft's hard limits so an out-of-range value can't
     * break dimension loading. A DimensionType requires min_y to be a multiple
     * of 16, min_y &gt;= -MAX_TOTAL_HEIGHT, and min_y + height &lt;= MAX_TOTAL_HEIGHT.
     */
    public static int clampMinY(int minY, int height) {
        if ((minY & 15) != 0) {
            int snapped = (minY >> 4) << 4; // floor toward -inf, stays a multiple of 16
            ArcadiaSpawnMod.LOGGER.warn("min_y {} must be a multiple of 16, snapped to {}.", minY, snapped);
            minY = snapped;
        }
        if (minY < -MAX_TOTAL_HEIGHT) {
            ArcadiaSpawnMod.LOGGER.warn("min_y {} below floor {}, clamping.", minY, -MAX_TOTAL_HEIGHT);
            minY = -MAX_TOTAL_HEIGHT;
        }
        if (minY + height > MAX_TOTAL_HEIGHT) {
            int newMinY = ((MAX_TOTAL_HEIGHT - height) >> 4) << 4;
            ArcadiaSpawnMod.LOGGER.warn("min_y {} + height {} exceeds {}, lowering min_y to {}.",
                    minY, height, MAX_TOTAL_HEIGHT, newMinY);
            minY = newMinY;
        }
        return minY;
    }

    /**
     * Parses "count*block_id" layer entries into flat-world layers, dropping the entries
     * whose block does not resolve so one typo costs a layer instead of a world load.
     */
    public static List<FlatLayerInfo> parseLayersPublic(List<? extends String> layerStrings) {
        List<FlatLayerInfo> layers = new ArrayList<>();
        if (layerStrings == null) return layers;

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
                    ArcadiaSpawnMod.LOGGER.error("Invalid block ID in layer config: {}", blockId);
                    continue;
                }

                Block block = BuiltInRegistries.BLOCK.get(loc);
                boolean isAir = block == Blocks.AIR;
                boolean meantAir = "minecraft:air".equals(blockId) || "air".equals(blockId);

                if (isAir && !meantAir) {
                    ArcadiaSpawnMod.LOGGER.error("Block '{}' not found (resolved to AIR). Skipping.", blockId);
                    continue;
                }

                layers.add(new FlatLayerInfo(count, block));
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.error("Failed to parse layer entry: {}", s, e);
            }
        }
        return layers;
    }
}
