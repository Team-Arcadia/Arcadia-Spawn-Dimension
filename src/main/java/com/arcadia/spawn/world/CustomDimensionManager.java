package com.arcadia.spawn.world;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.util.InputValidation;
import com.arcadia.spawn.util.SafeFileIO;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages user-created custom dimensions ("arcadia_custom:&lt;id&gt;").
 *
 * Definitions are JSON files under config/arcadia/spawn/dimensions/. They are the
 * source of truth an admin edits; the JSON Minecraft actually reads is derived from
 * them by {@link CustomDimensionPack}, which emits a real data pack and hands it to
 * the server pack repository at startup.
 *
 * Dimension types and level stems live in DATA PACK registries. Those are rebuilt
 * from data packs on every world load and are never visited by {@code RegisterEvent},
 * which only fires for the static registries in {@code BuiltInRegistries}. That is also
 * why creation requires a server restart: the level stem has to exist before the server
 * builds its levels.
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

    private static final String PURGE_PREFIX = "_purge_";
    private static final String PURGE_SUFFIX = ".marker";

    private static final String DEFAULT_INFINIBURN = "#minecraft:infiniburn_overworld";
    private static final String DEFAULT_EFFECTS = "minecraft:overworld";
    private static final String DEFAULT_BIOME = "minecraft:the_void";

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
                        .sorted()
                        .forEach(CustomDimensionManager::loadOne);
            }

            ArcadiaSpawnMod.LOGGER.info("Loaded {} custom dimensions.", DEFINITIONS.size());
            writeManifest();
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load custom dimensions", e);
        }
    }

    private static void loadOne(Path file) {
        loadOne(file, 0);
    }

    private static void loadOne(Path file, int attempt) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            CustomDimensionDef def = GSON.fromJson(reader, CustomDimensionDef.class);
            if (def == null || def.id == null) {
                ArcadiaSpawnMod.LOGGER.warn("Skipping invalid dimension file {}", file.getFileName());
                return;
            }
            if (!InputValidation.isValidDimensionId(def.id)) {
                ArcadiaSpawnMod.LOGGER.warn("Skipping dimension with invalid id {}", def.id);
                return;
            }
            DEFINITIONS.put(def.id, def);
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load dimension file {}", file.getFileName(), e);

            // Auto-recovery from backup — single retry only. findLatestBackup always
            // returns the same newest .bak, so if that backup is ALSO corrupt this
            // would recurse forever (StackOverflowError). Cap at one attempt.
            if (attempt >= 1) {
                ArcadiaSpawnMod.LOGGER.error("Backup recovery exhausted for {}, skipping.", file.getFileName());
                return;
            }
            java.io.File backup = SafeFileIO.findLatestBackup(file);
            if (backup != null) {
                ArcadiaSpawnMod.LOGGER.warn("Attempting recovery from backup {}", backup.getName());
                try {
                    Files.copy(backup.toPath(), file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    loadOne(file, attempt + 1);
                } catch (IOException io) {
                    ArcadiaSpawnMod.LOGGER.error("Backup recovery failed for {}", file.getFileName(), io);
                }
            }
        }
    }

    // ── Create / Delete / List API (runtime — requires restart to apply) ───

    public static synchronized boolean exists(String id) {
        return DEFINITIONS.containsKey(id) || Files.exists(DIMENSIONS_DIR.resolve(id + ".json"));
    }

    public static synchronized Collection<CustomDimensionDef> list() {
        return List.copyOf(DEFINITIONS.values());
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
            // An id can be re-created after a "delete <id> true" whose purge has not run
            // yet. Drop that pending purge, or the next startup would wipe the world data
            // of the dimension we just re-created.
            cancelPurge(id);
            writeManifest();
            CustomDimensionPack.regenerate();
            return true;
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to create dimension {}", id, e);
            return false;
        }
    }

    /**
     * Removes the definition file so the dimension stops being emitted into the generated
     * data pack — it is gone from the world on the next restart.
     *
     * With purge=true a marker is recorded, and the world data under
     * &lt;world&gt;/dimensions/arcadia_custom/&lt;id&gt;/ is deleted by
     * {@link CustomDimensionPack#runPendingPurges} once no level holds it open: on server
     * shutdown, and again on the next startup if the server died before that.
     */
    public static synchronized boolean delete(String id, boolean purge) {
        if (!DEFINITIONS.containsKey(id) && !Files.exists(DIMENSIONS_DIR.resolve(id + ".json"))) return false;

        try {
            Path file = DIMENSIONS_DIR.resolve(id + ".json");
            Files.deleteIfExists(file);
            DEFINITIONS.remove(id);

            if (purge) schedulePurge(id);

            writeManifest();
            CustomDimensionPack.regenerate();
            return true;
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to delete dimension {}", id, e);
            return false;
        }
    }

    // ── Purge markers ──────────────────────────────────────────────────────

    private static void schedulePurge(String id) {
        try {
            Files.createDirectories(DIMENSIONS_DIR);
            Files.writeString(purgeMarker(id),
                    "Scheduled purge of <world>/dimensions/" + CUSTOM_NAMESPACE + "/" + id +
                            ". Deleted automatically on server shutdown, retried on the next startup.",
                    StandardCharsets.UTF_8);
            ArcadiaSpawnMod.LOGGER.warn("World data of custom dimension {} scheduled for purge.", id);
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Could not schedule purge for dimension {}", id, e);
        }
    }

    static synchronized void cancelPurge(String id) {
        try {
            Files.deleteIfExists(purgeMarker(id));
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.warn("Could not clear purge marker for {}: {}", id, e.getMessage());
        }
    }

    /** Ids whose world data is still waiting to be deleted. Never contains a live definition. */
    static synchronized List<String> pendingPurges() {
        if (!Files.exists(DIMENSIONS_DIR)) return List.of();
        List<String> ids = new ArrayList<>();
        try (var stream = Files.list(DIMENSIONS_DIR)) {
            stream.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(PURGE_PREFIX) && n.endsWith(PURGE_SUFFIX))
                    .map(n -> n.substring(PURGE_PREFIX.length(), n.length() - PURGE_SUFFIX.length()))
                    // The id is resolved against the world directory below, so re-validate it
                    // here: a hand-written marker must not be able to carry ".." out of it.
                    .filter(InputValidation::isValidDimensionId)
                    .filter(id -> !DEFINITIONS.containsKey(id))
                    .forEach(ids::add);
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.warn("Could not read pending dimension purges: {}", e.getMessage());
        }
        return ids;
    }

    private static Path purgeMarker(String id) {
        return DIMENSIONS_DIR.resolve(PURGE_PREFIX + id + PURGE_SUFFIX);
    }

    // ── Data pack JSON generation ──────────────────────────────────────────

    /** Serializes a definition to the vanilla {@code dimension_type} data pack format. */
    static JsonObject toDimensionTypeJson(CustomDimensionDef def) {
        int height = clampHeight(def.height);
        int logicalHeight = Math.max(0, Math.min(def.logicalHeight, height));
        int minY = DimensionRegistry.clampMinY(def.minY, height);

        JsonObject o = new JsonObject();
        // fixed_time is optional: present locks the sky, absent lets the day cycle run.
        if (def.timeLocked) o.addProperty("fixed_time", Math.floorMod(def.fixedTime, 24000L));
        o.addProperty("has_skylight", def.hasSkylight);
        o.addProperty("has_ceiling", def.hasCeiling);
        o.addProperty("ultrawarm", def.ultrawarm);
        o.addProperty("natural", def.natural);
        // Codec range is [1e-5, 3e7]; an out-of-range value aborts the whole world load.
        o.addProperty("coordinate_scale", clamp(def.coordinateScale, 1.0E-5D, 3.0E7D));
        o.addProperty("bed_works", def.bedWorks);
        o.addProperty("respawn_anchor_works", def.respawnAnchorWorks);
        o.addProperty("min_y", minY);
        o.addProperty("height", height);
        o.addProperty("logical_height", logicalHeight);
        o.addProperty("infiniburn", validTagOrDefault(def.infiniburn, DEFAULT_INFINIBURN));
        o.addProperty("effects", validIdOrDefault(def.effects, DEFAULT_EFFECTS));
        o.addProperty("ambient_light", clamp(def.ambientLight, 0.0D, 1.0D));
        o.addProperty("piglin_safe", def.piglinSafe);
        o.addProperty("has_raids", def.hasRaids);
        o.addProperty("monster_spawn_light_level", clamp(def.monsterSpawnLightLevel, 0, 15));
        o.addProperty("monster_spawn_block_light_limit", clamp(def.monsterSpawnBlockLightLimit, 0, 15));
        return o;
    }

    /** Serializes a definition to the vanilla {@code dimension} (level stem) data pack format. */
    static JsonObject toLevelStemJson(CustomDimensionDef def) {
        JsonObject settings = new JsonObject();
        settings.add("layers", layersJson(def));
        settings.addProperty("biome", validIdOrDefault(def.biome, DEFAULT_BIOME));
        settings.addProperty("lakes", false);
        settings.addProperty("features", def.generateFeatures);
        settings.add("structure_overrides", new JsonArray());

        JsonObject generator = new JsonObject();
        generator.addProperty("type", "minecraft:flat");
        generator.add("settings", settings);

        JsonObject root = new JsonObject();
        root.addProperty("type", CUSTOM_NAMESPACE + ":" + def.id);
        root.add("generator", generator);
        return root;
    }

    private static JsonArray layersJson(CustomDimensionDef def) {
        // parseLayersPublic drops entries whose block id does not resolve, so a typo in a
        // definition costs one layer instead of breaking the world load.
        List<FlatLayerInfo> layers = DimensionRegistry.parseLayersPublic(def.layers);
        if (layers.isEmpty()) {
            ArcadiaSpawnMod.LOGGER.warn("Custom dimension {} has no usable layer, falling back to bedrock.", def.id);
            layers = List.of(new FlatLayerInfo(1, Blocks.BEDROCK));
        }

        JsonArray arr = new JsonArray();
        for (FlatLayerInfo layer : layers) {
            JsonObject entry = new JsonObject();
            entry.addProperty("block", BuiltInRegistries.BLOCK.getKey(layer.getBlockState().getBlock()).toString());
            entry.addProperty("height", layer.getHeight());
            arr.add(entry);
        }
        return arr;
    }

    private static int clampHeight(int h) {
        if (h > 2032) h = 2032;
        if (h < 16) h = 16;
        return (h >> 4) << 4;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp(double v, double min, double max) {
        if (Double.isNaN(v)) return min;
        return Math.max(min, Math.min(max, v));
    }

    private static String validIdOrDefault(String value, String fallback) {
        if (value == null || ResourceLocation.tryParse(value) == null) {
            ArcadiaSpawnMod.LOGGER.warn("Invalid resource id {} in a custom dimension, using {}.", value, fallback);
            return fallback;
        }
        return value;
    }

    private static String validTagOrDefault(String value, String fallback) {
        if (value == null || !value.startsWith("#") || ResourceLocation.tryParse(value.substring(1)) == null) {
            ArcadiaSpawnMod.LOGGER.warn("Invalid block tag {} in a custom dimension, using {}.", value, fallback);
            return fallback;
        }
        return value;
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
            hint.addProperty("generated_pack",
                    "Regenerated from these files at every startup, do not edit by hand: " +
                    CustomDimensionPack.packRoot());
            root.add("cleanup", hint);

            Files.createDirectories(DIMENSIONS_DIR);
            SafeFileIO.writeAtomicWithBackup(MANIFEST, GSON.toJson(root));
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.warn("Could not write manifest: {}", e.getMessage());
        }
    }
}
