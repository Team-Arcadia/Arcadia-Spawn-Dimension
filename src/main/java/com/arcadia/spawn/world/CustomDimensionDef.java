package com.arcadia.spawn.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Serializable definition of a custom dimension.
 * Persisted as JSON under config/arcadia/spawn/dimensions/<id>.json.
 *
 * All fields default to safe values so partial JSON files still produce a
 * working dimension (Gson leaves missing fields at their default).
 */
public class CustomDimensionDef {

    public String id = "";

    // Dimension type properties
    public boolean ultrawarm = false;
    public boolean natural = true;
    public double coordinateScale = 1.0;
    public boolean hasSkylight = true;
    public boolean hasCeiling = false;
    public double ambientLight = 0.0;
    public int monsterSpawnLightLevel = 0;
    public int monsterSpawnBlockLightLimit = 0;
    public boolean piglinSafe = false;
    public boolean bedWorks = false;
    public boolean respawnAnchorWorks = false;
    public boolean hasRaids = false;
    public int logicalHeight = 384;
    public int minY = -64;
    public int height = 384;
    public String infiniburn = "#minecraft:infiniburn_overworld";
    public String effects = "minecraft:overworld";

    // Time
    public boolean timeLocked = false;
    public long fixedTime = 6000L;

    // World gen
    public String biome = "minecraft:the_void";
    public List<String> layers = new ArrayList<>(Arrays.asList(
            "1*minecraft:bedrock", "2*minecraft:dirt", "1*minecraft:grass_block"));
    public boolean generateFeatures = false;

    public CustomDimensionDef() {}

    public static CustomDimensionDef preset(String id, String preset) {
        CustomDimensionDef def = new CustomDimensionDef();
        def.id = id;

        if (preset == null) preset = "flat";
        switch (preset.toLowerCase()) {
            case "void" -> {
                def.layers = new ArrayList<>(List.of("1*minecraft:air"));
                def.biome = "minecraft:the_void";
                def.hasSkylight = true;
            }
            case "lobby" -> {
                def.layers = new ArrayList<>(Arrays.asList(
                        "1*minecraft:bedrock", "3*minecraft:stone", "1*minecraft:smooth_quartz"));
                def.biome = "minecraft:plains";
                def.timeLocked = true;
                def.fixedTime = 6000L;
                def.hasSkylight = true;
            }
            case "flat" -> {
                // already default
            }
            default -> {
                // unknown preset → flat default
            }
        }
        return def;
    }
}
