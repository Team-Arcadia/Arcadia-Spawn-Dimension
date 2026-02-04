package com.vyrriox.arcadiaspawn.lobby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<LobbyLocation> ALL_LOCATIONS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("arcadia/arcadialobbyspawn");

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
            if (files == null)
                return;

            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonElement json = GSON.fromJson(reader, JsonElement.class);
                    if (json != null && json.isJsonArray()) {
                        for (JsonElement element : json.getAsJsonArray()) {
                            JsonObject obj = element.getAsJsonObject();
                            String name = obj.get("name").getAsString();
                            String dimId = obj.get("dimension").getAsString();
                            double x = obj.get("x").getAsDouble();
                            double y = obj.get("y").getAsDouble();
                            double z = obj.get("z").getAsDouble();
                            float yaw = obj.get("yaw").getAsFloat();
                            float pitch = obj.get("pitch").getAsFloat();
                            String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                            String item = obj.has("item") ? obj.get("item").getAsString() : "minecraft:paper";

                            ALL_LOCATIONS.add(LobbyLocation.of(name, dimId, x, y, z, yaw, pitch, desc, item));
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
        // Remove existing if any (update behavior)
        ALL_LOCATIONS.removeIf(l -> l.name().equals(location.name()));
        ALL_LOCATIONS.add(location);
        saveDimension(location.dimension());
    }

    public static boolean removeLocation(String name) {
        // We need to find the dimension BEFORE removing to save the file
        LobbyLocation loc = getLocation(name);
        if (loc == null)
            return false;

        boolean removed = ALL_LOCATIONS.remove(loc);
        if (removed) {
            saveDimension(loc.dimension());
        }
        return removed;
    }

    // Compatibility method if needed, but we prefer the name-only one now since
    // names should be unique
    public static boolean removeLocation(String name, String dimensionId) {
        return removeLocation(name);
    }

    public static LobbyLocation getLocation(String name) {
        for (LobbyLocation loc : ALL_LOCATIONS) {
            if (loc.name().equalsIgnoreCase(name)) {
                return loc;
            }
        }
        return null;
    }

    public static void updateLocation(String name, LobbyLocation newLocation) {
        // Just use add, it handles overwrite
        addLocation(newLocation);
    }

    public static List<LobbyLocation> getLocations() {
        return new ArrayList<>(ALL_LOCATIONS);
    }

    private static void saveDimension(LobbyLocation newLoc) {
        saveDimension(newLoc.dimension());
    }

    private static void saveDimension(ResourceKey<Level> dimensionKey) {
        String dimId = dimensionKey.location().toString();
        String filename = dimensionKey.location().getPath() + ".json";
        File file = CONFIG_DIR.resolve(filename).toFile();

        List<LobbyLocation> toSave = new ArrayList<>();
        for (LobbyLocation loc : ALL_LOCATIONS) {
            if (loc.dimension().location().getPath().equals(dimensionKey.location().getPath())) {
                toSave.add(loc);
            }
        }

        // If empty, maybe delete file?
        if (toSave.isEmpty()) {
            if (file.exists())
                file.delete();
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
