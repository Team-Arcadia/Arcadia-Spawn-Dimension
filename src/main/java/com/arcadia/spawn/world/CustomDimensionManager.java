package com.arcadia.spawn.world;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.util.InputValidation;
import com.arcadia.spawn.util.SafeFileIO;
import com.google.gson.*;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages user-created custom dimensions ("arcadia_custom:<id>").
 *
 * Definitions are JSON files under config/arcadia/spawn/dimensions/.
 * Loaded once at game startup (RegisterEvent); creation requires server restart
 * to take effect — this is a NeoForge constraint, not a design choice.
 *
 * Manifest (_manifest.json) tracks all dimensions owned by this mod, so an admin
 * can audit / purge them after mod removal.
 *
 * Backward compatibility: this system is opt-in. If the dimensions/ folder is
 * empty or missing, nothing happens — existing servers stay identical.
 */
public final class CustomDimensionManager {

    public static final String CUSTOM_NAMESPACE = "arcadia_custom";

    private static final Path DIMENSIONS_DIR = FMLPaths.CONFIGDIR.get().resolve("arcadia/spawn/dimensions");
    private static final Path MANIFEST = DIMENSIONS_DIR.resolve("_manifest.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<String, CustomDimensionDef> DEFINITIONS = new LinkedHashMap<>();
    private static boolean loaded = false;

    private CustomDimensionManager() {}

    public static synchronized void loadAll() {
        if (loaded) return;
        loaded = true;
        DEFINITIONS.clear();

        try {
            if (!Files.exists(DIMENSIONS_DIR)) {
                Files.createDirectories(DIMENSIONS_DIR);
                return;
            }

            try (var stream = Files.list(DIMENSIONS_DIR)) {
                stream
                        .filter(p -> p.toString().endsWith(".json"))
                        .filter(p -> !p.getFileName().toString().startsWith("_"))
                        .forEach(CustomDimensionManager::loadOne);
            }

            ArcadiaSpawnMod.LOGGER.info("Loaded {} custom dimensions.", DEFINITIONS.size());
            writeManifest();
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load custom dimensions", e);
        }
    }

    private static void loadOne(Path file) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            CustomDimensionDef def = GSON.fromJson(reader, CustomDimensionDef.class);
            if (def == null || def.id == null) {
                ArcadiaSpawnMod.LOGGER.warn("Skipping invalid dimension file {}", file.getFileName());
                return;
            }
            if (!InputValidation.isValidDimensionId(def.id)) {
                ArcadiaSpawnMod.LOGGER.warn("Skipping dimension with invalid id '{}'", def.id);
                return;
            }
            DEFINITIONS.put(def.id, def);
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load dimension file {}", file.getFileName(), e);

            // Auto-recovery from backup
            java.io.File backup = SafeFileIO.findLatestBackup(file);
            if (backup != null) {
                ArcadiaSpawnMod.LOGGER.warn("Attempting recovery from backup {}", backup.getName());
                try {
                    Files.copy(backup.toPath(), file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    loadOne(file);
                } catch (IOException io) {
                    ArcadiaSpawnMod.LOGGER.error("Backup recovery failed for {}", file.getFileName(), io);
                }
            }
        }
    }

    public static void registerAllDimensionTypes(RegisterEvent.RegisterHelper<DimensionType> helper) {
        loadAll();
        for (CustomDimensionDef def : DEFINITIONS.values()) {
            try {
                ResourceKey<DimensionType> key = ResourceKey.create(Registries.DIMENSION_TYPE,
                        ResourceLocation.fromNamespaceAndPath(CUSTOM_NAMESPACE, def.id));
                helper.register(key, buildDimensionType(def));
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.error("Failed to register dimension type {}", def.id, e);
            }
        }
    }

    public static void registerAllLevelStems(RegisterEvent.RegisterHelper<LevelStem> helper, Registry<Biome> biomeRegistry) {
        for (CustomDimensionDef def : DEFINITIONS.values()) {
            try {
                ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM,
                        ResourceLocation.fromNamespaceAndPath(CUSTOM_NAMESPACE, def.id));
                helper.register(stemKey, buildLevelStem(def, biomeRegistry));
            } catch (Exception e) {
                ArcadiaSpawnMod.LOGGER.error("Failed to register level stem for {}", def.id, e);
            }
        }
    }

    private static DimensionType buildDimensionType(CustomDimensionDef def) {
        int height = clampHeight(def.height);
        int logicalHeight = Math.min(def.logicalHeight, height);

        return new DimensionType(
                def.timeLocked ? OptionalLong.of(def.fixedTime) : OptionalLong.empty(),
                def.hasSkylight, def.hasCeiling, def.ultrawarm, def.natural,
                def.coordinateScale, def.bedWorks, def.respawnAnchorWorks,
                def.minY, height, logicalHeight,
                TagKey.create(Registries.BLOCK, ResourceLocation.parse(def.infiniburn)),
                ResourceLocation.parse(def.effects),
                (float) def.ambientLight,
                new DimensionType.MonsterSettings(def.piglinSafe, def.hasRaids,
                        ConstantInt.of(def.monsterSpawnLightLevel), def.monsterSpawnBlockLightLimit)
        );
    }

    private static LevelStem buildLevelStem(CustomDimensionDef def, Registry<Biome> biomeRegistry) {
        ResourceLocation biomeId;
        try {
            biomeId = ResourceLocation.parse(def.biome);
        } catch (Exception e) {
            biomeId = ResourceLocation.fromNamespaceAndPath("minecraft", "the_void");
        }

        ResourceLocation finalBiomeId = biomeId;
        Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, biomeId))
                .orElseGet(() -> {
                    ArcadiaSpawnMod.LOGGER.warn("Biome '{}' missing for custom dim {}, using void.", finalBiomeId, def.id);
                    return biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
                });

        List<FlatLayerInfo> layers = DimensionRegistry.parseLayersPublic(def.layers);
        if (layers.isEmpty()) layers.add(new FlatLayerInfo(1, Blocks.BEDROCK));

        Holder<PlacedFeature> empty = DimensionRegistry.emptyPlacedFeature();
        FlatLevelGeneratorSettings settings = DimensionRegistry.createSettings(
                Optional.empty(), layers, false, def.generateFeatures,
                Optional.of(biomeHolder), biomeHolder, empty, empty);

        return new LevelStem(Holder.direct(buildDimensionType(def)), new FlatLevelSource(settings));
    }

    private static int clampHeight(int h) {
        if (h > 2032) h = 2032;
        if (h < 16) h = 16;
        return (h >> 4) << 4;
    }

    // ── Create / Delete / List API (runtime — requires restart to apply) ───

    public static synchronized boolean exists(String id) {
        return DEFINITIONS.containsKey(id) || Files.exists(DIMENSIONS_DIR.resolve(id + ".json"));
    }

    public static Collection<CustomDimensionDef> list() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    /** Creates a dimension definition file. Returns false if id is taken or invalid. */
    public static synchronized boolean create(String id, String preset, String biomeOverride) {
        if (!InputValidation.isValidDimensionId(id)) return false;
        if (exists(id)) return false;

        CustomDimensionDef def = CustomDimensionDef.preset(id, preset);
        if (biomeOverride != null && !biomeOverride.isBlank()) {
            try {
                ResourceLocation.parse(biomeOverride);
                def.biome = biomeOverride;
            } catch (Exception ignored) {}
        }

        try {
            Files.createDirectories(DIMENSIONS_DIR);
            Path file = DIMENSIONS_DIR.resolve(id + ".json");
            SafeFileIO.writeAtomicWithBackup(file, GSON.toJson(def));
            DEFINITIONS.put(id, def);
            writeManifest();
            return true;
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to create dimension {}", id, e);
            return false;
        }
    }

    /**
     * Removes the definition file (and optionally schedules world data purge).
     * The actual world data under <world>/dimensions/arcadia_custom/<id>/ is
     * left untouched unless purge=true and the user understands a restart is needed.
     */
    public static synchronized boolean delete(String id, boolean purge) {
        if (!DEFINITIONS.containsKey(id) && !Files.exists(DIMENSIONS_DIR.resolve(id + ".json"))) return false;

        try {
            Path file = DIMENSIONS_DIR.resolve(id + ".json");
            Files.deleteIfExists(file);
            DEFINITIONS.remove(id);

            if (purge) {
                Path purgeMarker = DIMENSIONS_DIR.resolve("_purge_" + id + ".marker");
                Files.writeString(purgeMarker, "Delete <world>/dimensions/" + CUSTOM_NAMESPACE + "/" + id + " after server stop.",
                        StandardCharsets.UTF_8);
                ArcadiaSpawnMod.LOGGER.warn("Purge marker created for {} — manual cleanup required after shutdown.", id);
            }

            writeManifest();
            return true;
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to delete dimension {}", id, e);
            return false;
        }
    }

    private static void writeManifest() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("mod", ArcadiaSpawnMod.MOD_ID);
            root.addProperty("namespace", CUSTOM_NAMESPACE);
            root.addProperty("generated", System.currentTimeMillis());
            JsonArray arr = new JsonArray();
            for (String id : DEFINITIONS.keySet()) arr.add(id);
            root.add("dimensions", arr);

            JsonObject hint = new JsonObject();
            hint.addProperty("on_mod_removal",
                    "If this mod is uninstalled, manually delete the following from your world save: " +
                    "world/dimensions/" + CUSTOM_NAMESPACE + "/<id> for each listed dimension.");
            root.add("cleanup", hint);

            Files.createDirectories(DIMENSIONS_DIR);
            SafeFileIO.writeAtomicWithBackup(MANIFEST, GSON.toJson(root));
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.warn("Could not write manifest: {}", e.getMessage());
        }
    }
}
