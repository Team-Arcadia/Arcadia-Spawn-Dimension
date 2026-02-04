package com.vyrriox.arcadiaspawn.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class SpawnConfig {
        public static final ModConfigSpec SPEC;
        public static final Common COMMON;

        public static class Common {
                // Dimension Type Settings
                public final ModConfigSpec.ConfigValue<Boolean> ultrawarm;
                public final ModConfigSpec.ConfigValue<Boolean> natural;
                public final ModConfigSpec.ConfigValue<Double> coordinateScale;
                public final ModConfigSpec.ConfigValue<Boolean> hasSkylight;
                public final ModConfigSpec.ConfigValue<Boolean> hasCeiling;
                public final ModConfigSpec.ConfigValue<Double> ambientLight;
                public final ModConfigSpec.ConfigValue<Integer> monsterSpawnLightLevel;
                public final ModConfigSpec.ConfigValue<Integer> monsterSpawnBlockLightLimit;
                public final ModConfigSpec.ConfigValue<Boolean> piglinSafe;
                public final ModConfigSpec.ConfigValue<Boolean> bedWorks;
                public final ModConfigSpec.ConfigValue<Boolean> respawnAnchorWorks;
                public final ModConfigSpec.ConfigValue<Boolean> hasRaids;
                public final ModConfigSpec.ConfigValue<Integer> logicalHeight;
                public final ModConfigSpec.ConfigValue<Integer> minY;
                public final ModConfigSpec.ConfigValue<Integer> height;
                public final ModConfigSpec.ConfigValue<String> infiniburn;
                public final ModConfigSpec.ConfigValue<String> effects;

                // Time Settings
                public final ModConfigSpec.ConfigValue<Boolean> timeLocked;
                public final ModConfigSpec.ConfigValue<Long> fixedTime;

                // Weather Settings
                public final ModConfigSpec.ConfigValue<Boolean> allowRain;
                public final ModConfigSpec.ConfigValue<Boolean> allowThunder;

                // World Gen / Biome Settings
                public final ModConfigSpec.ConfigValue<String> biome;
                public final ModConfigSpec.ConfigValue<List<? extends String>> flatLayers;
                public final ModConfigSpec.ConfigValue<Boolean> latesAndFeatures; // "structures" usually

                // Mob Spawning
                public final ModConfigSpec.ConfigValue<Boolean> spawnMonsters;
                public final ModConfigSpec.ConfigValue<Boolean> spawnCreatures;
                public final ModConfigSpec.ConfigValue<Boolean> spawnAmbient;
                public final ModConfigSpec.ConfigValue<Boolean> spawnWaterCreatures;
                public final ModConfigSpec.ConfigValue<Boolean> spawnWaterAmbient;
                public final ModConfigSpec.ConfigValue<Boolean> spawnMisc;
                public final ModConfigSpec.ConfigValue<Boolean> spawnUndergroundWaterCreatures;
                public final ModConfigSpec.ConfigValue<Boolean> spawnAxolotls;

                // RTP Settings
                public final ModConfigSpec.ConfigValue<Integer> rtpRadius;
                public final ModConfigSpec.ConfigValue<Integer> rtpMaxUsage;

                Common(ModConfigSpec.Builder builder) {
                        builder.push("Dimension Properties");
                        builder.comment("Configuration for the fundamental properties of the spawn dimension.");

                        ultrawarm = builder.comment("If true, water evaporates like in the Nether.")
                                        .define("ultrawarm", false);

                        natural = builder.comment(
                                        "If false, compasses spin randomly and beds might explode (if enabled).")
                                        .define("natural", true);

                        coordinateScale = builder
                                        .comment("The multiplier for coordinates when traveling to this dimension.")
                                        .define("coordinate_scale", 1.0);

                        hasSkylight = builder.comment("If the dimension has a sky and skylight.")
                                        .define("has_skylight", true);

                        hasCeiling = builder.comment("If the dimension has a bedrock ceiling.")
                                        .define("has_ceiling", false);

                        ambientLight = builder.comment("Amount of ambient light (0.0 to 1.0).")
                                        .defineInRange("ambient_light", 0.0, 0.0, 1.0);

                        monsterSpawnLightLevel = builder.comment("Light level at which monsters can spawn.")
                                        .defineInRange("monster_spawn_light_level", 0, 0, 15);

                        monsterSpawnBlockLightLimit = builder.comment("Block light limit for monster spawning.")
                                        .defineInRange("monster_spawn_block_light_limit", 0, 0, 15);

                        piglinSafe = builder.comment("If Piglins do not zombify in this dimension.")
                                        .define("piglin_safe", false);

                        bedWorks = builder.comment("If beds can be slept in without exploding.")
                                        .define("bed_works", false);

                        respawnAnchorWorks = builder.comment("If respawn anchors can be charged and used.")
                                        .define("respawn_anchor_works", false);

                        hasRaids = builder.comment("If raids can trigger in this dimension.")
                                        .define("has_raids", false);

                        logicalHeight = builder.comment("The logical height of the world.")
                                        .defineInRange("logical_height", 384, 0, 2048);

                        minY = builder.comment("The minimum Y coordinate.")
                                        .defineInRange("min_y", -64, -2048, 2048);

                        height = builder.comment("The total height of the world (must be multiple of 16).")
                                        .defineInRange("height", 384, 16, 2048);

                        infiniburn = builder.comment("Tag for blocks that burn infinitely.")
                                        .define("infiniburn", "#minecraft:infiniburn_overworld");

                        effects = builder.comment(
                                        "Dimension effects path (e.g., minecraft:overworld, minecraft:the_nether).")
                                        .define("effects", "minecraft:overworld");
                        builder.pop();

                        builder.push("Time Management");
                        timeLocked = builder.comment("If true, time is frozen at 'Fixed Time'.")
                                        .define("time_locked", false);

                        fixedTime = builder.comment(
                                        "The time of day to freeze the dimension at (if time_locked is true). 6000 = Noon, 18000 = Midnight.")
                                        .defineInRange("fixed_time", 6000L, 0L, 24000L);
                        builder.pop();

                        builder.push("Weather");
                        allowRain = builder.comment(
                                        "If rain is allowed in the dimension (requires has_skylight=true usually).")
                                        .define("allow_rain", false);

                        allowThunder = builder.comment("If thunder is allowed.")
                                        .define("allow_thunder", false);
                        builder.pop();

                        builder.push("World Generation");
                        biome = builder.comment("The ID of the biome to use for the single-biome generation.")
                                        .define("biome", "minecraft:the_void");

                        // Default layers: 1 Bedrock, 2 Dirt, 1 Grass Block
                        flatLayers = builder.comment(
                                        "The layers of the flat world. Format: 'block_id' or 'count*block_id'. Ordered from bottom to top.")
                                        .defineList("layers",
                                                        Arrays.asList("1*minecraft:bedrock", "2*minecraft:dirt",
                                                                        "1*minecraft:grass_block"),
                                                        entry -> entry instanceof String);

                        latesAndFeatures = builder.comment(
                                        "Allow lakes and features (structures are disabled by mod logic regardless of this, but features are configurable).")
                                        .define("generate_features", false);
                        builder.pop();

                        builder.push("Mob Spawning Rules");
                        builder.comment("Enable or disable spawning for specific mob categories.");
                        builder.comment("NOTE: In version 1.3.0, setting this to FALSE blocks ALL spawns (Natural, Spawners, Eggs).");
                        builder.comment("WORKAROUND: To allow Spawners but block Natural, set this to TRUE and ensure 'biome' is set to 'minecraft:the_void' (which has no natural spawns).");
                        spawnMonsters = builder.define("spawn_monsters", false);
                        spawnCreatures = builder.define("spawn_creatures", false);
                        spawnAmbient = builder.define("spawn_ambient", false);
                        spawnWaterCreatures = builder.define("spawn_water_creatures", false);
                        spawnWaterAmbient = builder.define("spawn_water_ambient", false);
                        spawnMisc = builder.define("spawn_misc", false);
                        spawnUndergroundWaterCreatures = builder.define("spawn_underground_water_creatures", false);
                        spawnAxolotls = builder.define("spawn_axolotls", false);
                        builder.pop();

                        builder.push("RTP Settings");
                        rtpRadius = builder.comment("The maximum radius from (0,0) for the Random Teleport.")
                                        .defineInRange("rtp_radius", 50000, 1000, 1000000);
                        rtpMaxUsage = builder.comment(
                                        "The maximum number of times a player can use /arcadiartp before reusing their last spot.")
                                        .defineInRange("rtp_max_usage", 1, 0, 100);
                        builder.pop();
                }
        }

        static {
                Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
                SPEC = specPair.getRight();
                COMMON = specPair.getLeft();
        }
}
