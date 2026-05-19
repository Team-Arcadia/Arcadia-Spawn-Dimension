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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

            for (File file : files) {
                if (!loadFile(file)) {
                    File backup = SafeFileIO.findLatestBackup(file.toPath());
                    if (backup != null) {
                        ArcadiaSpawnMod.LOGGER.warn("Lobby file {} unreadable, restoring from {}", file.getName(), backup.getName());
                        try {
                            Files.copy(backup.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            loadFile(file);
                        } catch (IOException io) {
                            ArcadiaSpawnMod.LOGGER.error("Failed to restore lobby file {} from backup", file.getName(), io);
                        }
                    }
                }
            }
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
                ALL_LOCATIONS.add(loc);
                INDEX.put(loc.name().toLowerCase(Locale.ROOT), loc);
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

    private static void saveDimension(ResourceKey<Level> dimensionKey) {
        String filename = dimensionKey.location().getPath() + ".json";
        Path target = CONFIG_DIR.resolve(filename);

        List<LobbyLocation> toSave = new ArrayList<>();
        for (LobbyLocation loc : ALL_LOCATIONS) {
            if (loc.dimension().location().getPath().equals(dimensionKey.location().getPath())) {
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
            ArcadiaSpawnMod.LOGGER.error("Failed to save lobby file {}", filename, e);
        }
    }
}
