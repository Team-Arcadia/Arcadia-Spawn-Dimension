package com.vyrriox.arcadiaspawn.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author vyrriox
 * Configuration for the slot bypass system.
 * Allows specific permission holders to join even when the server is full.
 */
public class SlotBypassConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    public static class Values {
        public final ModConfigSpec.ConfigValue<Boolean> enabled;
        public final ModConfigSpec.ConfigValue<Integer> maxSlots;
        public final ModConfigSpec.ConfigValue<String> kickMessage;

        Values(ModConfigSpec.Builder builder) {
            builder.push("Slot Bypass");
            builder.comment("Configuration for the slot bypass system.",
                    "Players with the permission 'arcadia_spawn.slots.bypass' can join even when the server is full.",
                    "Assign this permission to LuckPerms groups/players as needed.");

            enabled = builder.comment("Enable or disable the slot bypass system.")
                    .define("enabled", false);

            maxSlots = builder.comment(
                    "Maximum number of player slots before bypass is required.",
                    "Players without the bypass permission will be kicked when the server reaches this number.")
                    .defineInRange("max_slots", 20, 1, 1000);

            kickMessage = builder.comment(
                    "The kick message shown to players without bypass permission when the server is full.",
                    "Supports Minecraft formatting codes with '&' prefix.")
                    .define("kick_message", "&cThe server is full! &7Only VIP players can join right now.");

            builder.pop();
        }
    }

    static {
        Pair<Values, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Values::new);
        SPEC = specPair.getRight();
        VALUES = specPair.getLeft();
    }
}
