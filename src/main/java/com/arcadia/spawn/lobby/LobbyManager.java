package com.arcadia.spawn.lobby;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.google.gson.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LobbyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<LobbyLocation> ALL_LOCATIONS = new CopyOnWriteArrayList<>();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("arcadia/spawn/lobbies");

    public static void init() {
        reload();
    }

    public static void reload() {
        ALL_LOCATIONS.clear();
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            File[] files = CONFIG_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null) return;

            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonElement json = GSON.fromJson(reader, JsonElement.class);
                    if (json != null && json.isJsonArray()) {
                        for (JsonElement element : json.getAsJsonArray()) {
                            JsonObject obj = element.getAsJsonObject();
                            ALL_LOCATIONS.add(LobbyLocation.of(
                                    obj.get("name").getAsString(),
                                    obj.get("dimension").getAsString(),
                                    obj.get("x").getAsDouble(),
                                    obj.get("y").getAsDouble(),
                                    obj.get("z").getAsDouble(),
                                    obj.get("yaw").getAsFloat(),
                                    obj.get("pitch").getAsFloat(),
                                    obj.has("description") ? obj.get("description").getAsString() : "",
                                    obj.has("item") ? obj.get("item").getAsString() : "minecraft:paper"
                            ));
                        }
                    }
                } catch (Exception e) {
                    ArcadiaSpawnMod.LOGGER.error("Failed to load lobby config: {}", file.getName(), e);
                }
            }
            ArcadiaSpawnMod.LOGGER.info("Loaded {} lobby locations.", ALL_LOCATIONS.size());
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to initialize lobby manager", e);
        }
    }

    public static void addLocation(LobbyLocation location) {
        ALL_LOCATIONS.removeIf(l -> l.name().equalsIgnoreCase(location.name()));
        ALL_LOCATIONS.add(location);
        saveDimension(location.dimension());
    }

    public static boolean removeLocation(String name) {
        LobbyLocation loc = getLocation(name);
        if (loc == null) return false;
        boolean removed = ALL_LOCATIONS.remove(loc);
        if (removed) saveDimension(loc.dimension());
        return removed;
    }

    public static LobbyLocation getLocation(String name) {
        for (LobbyLocation loc : ALL_LOCATIONS) {
            if (loc.name().equalsIgnoreCase(name)) return loc;
        }
        return null;
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
        File file = CONFIG_DIR.resolve(filename).toFile();

        List<LobbyLocation> toSave = new ArrayList<>();
        for (LobbyLocation loc : ALL_LOCATIONS) {
            if (loc.dimension().location().getPath().equals(dimensionKey.location().getPath())) {
                toSave.add(loc);
            }
        }

        if (toSave.isEmpty()) {
            if (file.exists()) file.delete();
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

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(array, writer);
        } catch (IOException e) {
            ArcadiaSpawnMod.LOGGER.error("Failed to save lobby location to {}", file.getName(), e);
        }
    }
}
