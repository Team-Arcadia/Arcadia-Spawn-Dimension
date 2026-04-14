package com.arcadia.spawn.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class SpawnConfig {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    public static class Common {
        // Dimension Type
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

        // Time
        public final ModConfigSpec.ConfigValue<Boolean> timeLocked;
        public final ModConfigSpec.ConfigValue<Long> fixedTime;

        // Weather
        public final ModConfigSpec.ConfigValue<Boolean> allowRain;
        public final ModConfigSpec.ConfigValue<Boolean> allowThunder;

        // World Gen
        public final ModConfigSpec.ConfigValue<String> biome;
        public final ModConfigSpec.ConfigValue<List<? extends String>> flatLayers;
        public final ModConfigSpec.ConfigValue<Boolean> latesAndFeatures;

        // Mob Spawning
        public final ModConfigSpec.ConfigValue<Boolean> spawnMonsters;
        public final ModConfigSpec.ConfigValue<Boolean> spawnCreatures;
        public final ModConfigSpec.ConfigValue<Boolean> spawnAmbient;
        public final ModConfigSpec.ConfigValue<Boolean> spawnWaterCreatures;
        public final ModConfigSpec.ConfigValue<Boolean> spawnWaterAmbient;
        public final ModConfigSpec.ConfigValue<Boolean> spawnMisc;
        public final ModConfigSpec.ConfigValue<Boolean> spawnUndergroundWaterCreatures;
        public final ModConfigSpec.ConfigValue<Boolean> spawnAxolotls;

        // RTP
        public final ModConfigSpec.ConfigValue<Integer> rtpRadius;
        public final ModConfigSpec.ConfigValue<Integer> rtpMaxUsage;
        public final ModConfigSpec.ConfigValue<Integer> rtpCooldownSeconds;
        public final ModConfigSpec.ConfigValue<Integer> rtpWarmupTicks;
        public final ModConfigSpec.ConfigValue<Integer> rtpMaxAttempts;

        // Teleport
        public final ModConfigSpec.ConfigValue<Integer> spawnTpWarmupTicks;
        public final ModConfigSpec.ConfigValue<Integer> spawnTpCooldownSeconds;
        public final ModConfigSpec.ConfigValue<Integer> lobbyTpWarmupTicks;
        public final ModConfigSpec.ConfigValue<Integer> lobbyTpCooldownSeconds;

        // First Join
        public final ModConfigSpec.ConfigValue<Boolean> forceSpawnOnFirstJoin;
        public final ModConfigSpec.ConfigValue<Boolean> forceSpawnOnRespawn;

        Common(ModConfigSpec.Builder builder) {
            builder.push("Dimension Properties");
            builder.comment("Configuration for the spawn dimension properties.");
            ultrawarm = builder.comment("If true, water evaporates like in the Nether.")
                    .define("ultrawarm", false);
            natural = builder.comment("If false, compasses spin randomly.")
                    .define("natural", true);
            coordinateScale = builder.comment("Coordinate scale multiplier.")
                    .define("coordinate_scale", 1.0);
            hasSkylight = builder.comment("If the dimension has sky and skylight.")
                    .define("has_skylight", true);
            hasCeiling = builder.comment("If the dimension has a bedrock ceiling.")
                    .define("has_ceiling", false);
            ambientLight = builder.comment("Ambient light level (0.0 to 1.0).")
                    .defineInRange("ambient_light", 0.0, 0.0, 1.0);
            monsterSpawnLightLevel = builder.comment("Light level for monster spawning.")
                    .defineInRange("monster_spawn_light_level", 0, 0, 15);
            monsterSpawnBlockLightLimit = builder.comment("Block light limit for monster spawning.")
                    .defineInRange("monster_spawn_block_light_limit", 0, 0, 15);
            piglinSafe = builder.comment("If Piglins do not zombify.")
                    .define("piglin_safe", false);
            bedWorks = builder.comment("If beds work without exploding.")
                    .define("bed_works", false);
            respawnAnchorWorks = builder.comment("If respawn anchors work.")
                    .define("respawn_anchor_works", false);
            hasRaids = builder.comment("If raids can trigger.")
                    .define("has_raids", false);
            logicalHeight = builder.comment("Logical world height.")
                    .defineInRange("logical_height", 384, 0, 2048);
            minY = builder.comment("Minimum Y coordinate.")
                    .defineInRange("min_y", -64, -2048, 2048);
            height = builder.comment("Total world height (multiple of 16).")
                    .defineInRange("height", 384, 16, 2048);
            infiniburn = builder.comment("Tag for infinitely burning blocks.")
                    .define("infiniburn", "#minecraft:infiniburn_overworld");
            effects = builder.comment("Dimension effects (e.g. minecraft:overworld).")
                    .define("effects", "minecraft:overworld");
            builder.pop();

            builder.push("Time Management");
            timeLocked = builder.comment("If true, time is frozen at 'fixed_time'.")
                    .define("time_locked", false);
            fixedTime = builder.comment("Fixed time value (6000 = Noon, 18000 = Midnight).")
                    .defineInRange("fixed_time", 6000L, 0L, 24000L);
            builder.pop();

            builder.push("Weather");
            allowRain = builder.comment("Allow rain in the dimension.")
                    .define("allow_rain", false);
            allowThunder = builder.comment("Allow thunder in the dimension.")
                    .define("allow_thunder", false);
            builder.pop();

            builder.push("World Generation");
            biome = builder.comment("Biome ID for single-biome generation.")
                    .define("biome", "minecraft:the_void");
            flatLayers = builder.comment("Flat world layers. Format: 'count*block_id'. Bottom to top.")
                    .defineList("layers",
                            Arrays.asList("1*minecraft:bedrock", "2*minecraft:dirt", "1*minecraft:grass_block"),
                            entry -> entry instanceof String);
            latesAndFeatures = builder.comment("Allow lakes and features generation.")
                    .define("generate_features", false);
            builder.pop();

            builder.push("Mob Spawning Rules");
            builder.comment("Enable or disable spawning per mob category.");
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
            rtpRadius = builder.comment("Maximum radius from (0,0) for Random Teleport.")
                    .defineInRange("rtp_radius", 50000, 1000, 1000000);
            rtpMaxUsage = builder.comment("Maximum RTP uses before reusing last position.")
                    .defineInRange("rtp_max_usage", 1, 0, 100);
            rtpCooldownSeconds = builder.comment("Cooldown between RTP uses (seconds).")
                    .defineInRange("rtp_cooldown_seconds", 60, 0, 3600);
            rtpWarmupTicks = builder.comment("Warmup delay before RTP teleport (ticks, 20 = 1s).")
                    .defineInRange("rtp_warmup_ticks", 60, 0, 200);
            rtpMaxAttempts = builder.comment("Maximum attempts to find a safe position.")
                    .defineInRange("rtp_max_attempts", 50, 10, 200);
            builder.pop();

            builder.push("Teleport Settings");
            spawnTpWarmupTicks = builder.comment("Warmup ticks for /spawn teleport (20 = 1s).")
                    .defineInRange("spawn_tp_warmup_ticks", 60, 0, 200);
            spawnTpCooldownSeconds = builder.comment("Cooldown for /spawn (seconds).")
                    .defineInRange("spawn_tp_cooldown_seconds", 30, 0, 3600);
            lobbyTpWarmupTicks = builder.comment("Warmup ticks for lobby teleport.")
                    .defineInRange("lobby_tp_warmup_ticks", 40, 0, 200);
            lobbyTpCooldownSeconds = builder.comment("Cooldown for lobby teleport (seconds).")
                    .defineInRange("lobby_tp_cooldown_seconds", 15, 0, 3600);
            builder.pop();

            builder.push("First Join");
            forceSpawnOnFirstJoin = builder.comment("Teleport new players to spawn dimension on first join.")
                    .define("force_spawn_on_first_join", true);
            forceSpawnOnRespawn = builder.comment("Teleport players to spawn on respawn (no bed/anchor).")
                    .define("force_spawn_on_respawn", false);
            builder.pop();
        }
    }

    static {
        Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}
