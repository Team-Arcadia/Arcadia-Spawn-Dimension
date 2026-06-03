package com.arcadia.spawn.lobby;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.util.SafeFileIO;
import com.google.gson.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<LobbyLocation> ALL_LOCATIONS = new CopyOnWriteArrayList<>();
    private static final Map<String, LobbyLocation> INDEX = new ConcurrentHashMap<>();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("arcadia/spawn/lobbies");

    public static void init() {
        reload();
    }

    public static void reload() {
        ALL_LOCATIONS.clear();
        INDEX.clear();
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            File[] files = CONFIG_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null) return;

            List<File> loadedFiles = new ArrayList<>();
            for (File file : files) {
                boolean ok = loadFile(file);
                if (!ok) {
                    File backup = SafeFileIO.findLatestBackup(file.toPath());
                    if (backup != null) {
                        ArcadiaSpawnMod.LOGGER.warn("Lobby file {} unreadable, restoring from {}", file.getName(), backup.getName());
                        try {
                            Files.copy(backup.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            ok = loadFile(file);
                        } catch (IOException io) {
                            ArcadiaSpawnMod.LOGGER.error("Failed to restore lobby file {} from backup", file.getName(), io);
                        }
                    }
                }
                if (ok) loadedFiles.add(file);
            }
            migrateLegacyFilenames(loadedFiles);
            ArcadiaSpawnMod.LOGGER.info("Loaded {} lobby locations.", ALL_LOCATIONS.size());
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to initialize lobby manager", e);
        }
    }

    private static boolean loadFile(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);
            if (json == null || !json.isJsonArray()) return false;
            for (JsonElement element : json.getAsJsonArray()) {
                JsonObject obj = element.getAsJsonObject();
                LobbyLocation loc = LobbyLocation.of(
                        obj.get("name").getAsString(),
                        obj.get("dimension").getAsString(),
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("z").getAsDouble(),
                        obj.get("yaw").getAsFloat(),
                        obj.get("pitch").getAsFloat(),
                        obj.has("description") ? obj.get("description").getAsString() : "",
                        obj.has("item") ? obj.get("item").getAsString() : "minecraft:paper"
                );
                String nameKey = loc.name().toLowerCase(Locale.ROOT);
                // Skip duplicates so a lingering legacy file alongside its migrated
                // namespaced counterpart can't surface the same lobby twice.
                if (INDEX.containsKey(nameKey)) continue;
                ALL_LOCATIONS.add(loc);
                INDEX.put(nameKey, loc);
            }
            return true;
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to load lobby config: {}", file.getName(), e);
            return false;
        }
    }

    public static void addLocation(LobbyLocation location) {
        String key = location.name().toLowerCase(Locale.ROOT);
        LobbyLocation existing = INDEX.remove(key);
        if (existing != null) ALL_LOCATIONS.remove(existing);
        ALL_LOCATIONS.add(location);
        INDEX.put(key, location);
        saveDimension(location.dimension());
    }

    public static boolean removeLocation(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        LobbyLocation loc = INDEX.remove(key);
        if (loc == null) return false;
        ALL_LOCATIONS.remove(loc);
        saveDimension(loc.dimension());
        return true;
    }

    public static LobbyLocation getLocation(String name) {
        return INDEX.get(name.toLowerCase(Locale.ROOT));
    }

    public static void updateLocation(String name, LobbyLocation newLocation) {
        addLocation(newLocation);
    }

    public static List<LobbyLocation> getLocations() {
        return new ArrayList<>(ALL_LOCATIONS);
    }

    public static int getLocationCount() {
        return ALL_LOCATIONS.size();
    }

    /**
     * Canonical on-disk file name for a dimension's lobbies, namespaced so two
     * dimensions sharing a path across namespaces (e.g. {@code minecraft:lobby} and
     * {@code arcadia:lobby}) can't collide on the same file.
     */
    private static String canonicalFilename(ResourceKey<Level> dimensionKey) {
        return dimensionKey.location().toString().replace(':', '.') + ".json";
    }

    private static void saveDimension(ResourceKey<Level> dimensionKey) {
        Path target = CONFIG_DIR.resolve(canonicalFilename(dimensionKey));

        List<LobbyLocation> toSave = new ArrayList<>();
        for (LobbyLocation loc : ALL_LOCATIONS) {
            // Match on the FULL ResourceKey, not just the path, so distinct
            // dimensions are never grouped into the same file.
            if (loc.dimension().equals(dimensionKey)) {
                toSave.add(loc);
            }
        }

        if (toSave.isEmpty()) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) {}
            return;
        }

        JsonArray array = new JsonArray();
        for (LobbyLocation loc : toSave) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", loc.name());
            obj.addProperty("dimension", loc.dimension().location().toString());
            obj.addProperty("description", loc.description());
            obj.addProperty("item", loc.item());
            obj.addProperty("x", loc.x());
            obj.addProperty("y", loc.y());
            obj.addProperty("z", loc.z());
            obj.addProperty("yaw", loc.yaw());
            obj.addProperty("pitch", loc.pitch());
            array.add(obj);
        }

        try {
            SafeFileIO.writeAtomicWithBackup(target, GSON.toJson(array));
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to save lobby file {}", target.getFileName(), e);
        }
    }

    /**
     * One-time migration from the legacy path-only file naming ({@code <path>.json})
     * to the namespaced scheme ({@code <namespace>.<path>.json}). Runs after load:
     * every successfully-loaded location already lives in memory, so re-saving each
     * dimension to its canonical file and deleting the old files loses no data.
     * Only files that loaded successfully are removed, so a corrupt/unreadable file
     * is never deleted. No-op once every file is already canonical.
     */
    private static void migrateLegacyFilenames(List<File> loadedFiles) {
        Set<String> canonical = new HashSet<>();
        Set<ResourceKey<Level>> dimensions = new LinkedHashSet<>();
        for (LobbyLocation loc : ALL_LOCATIONS) {
            canonical.add(canonicalFilename(loc.dimension()));
            dimensions.add(loc.dimension());
        }

        List<File> legacy = new ArrayList<>();
        for (File f : loadedFiles) {
            if (!canonical.contains(f.getName())) legacy.add(f);
        }
        if (legacy.isEmpty()) return; // already canonical

        for (ResourceKey<Level> dim : dimensions) saveDimension(dim);

        for (File f : legacy) {
            if (f.delete()) {
                ArcadiaSpawnMod.LOGGER.info("Migrated lobby file {} to namespaced format.", f.getName());
            } else {
                ArcadiaSpawnMod.LOGGER.warn("Could not remove legacy lobby file {} after migration.", f.getName());
            }
        }
    }
}
