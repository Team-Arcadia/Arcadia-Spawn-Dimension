package com.arcadia.spawn.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author vyrriox
 * Slot bypass configuration.
 * Players with 'arcadia_spawn.slots.bypass' permission can join when server is full.
 */
public class SlotBypassConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    public static class Values {
        public final ModConfigSpec.ConfigValue<Boolean> enabled;
        public final ModConfigSpec.ConfigValue<Integer> maxSlots;
        public final ModConfigSpec.ConfigValue<String> kickMessage;
        public final ModConfigSpec.ConfigValue<Boolean> fakeMaxSlotsEnabled;
        public final ModConfigSpec.ConfigValue<Boolean> hideJoinLeaveMessages;

        Values(ModConfigSpec.Builder builder) {
            builder.push("Slot Bypass");
            builder.comment(
                    "Slot bypass system configuration.",
                    "Players with 'arcadia_spawn.slots.bypass' can connect when server is full.",
                    "Assign this permission via LuckPerms.");

            enabled = builder.comment("Enable or disable the slot bypass system.")
                    .define("enabled", false);

            maxSlots = builder.comment(
                    "Maximum player count before bypass is required.",
                    "Players without bypass permission are kicked when this limit is reached.")
                    .defineInRange("max_slots", 20, 1, 1000);

            kickMessage = builder.comment(
                    "Kick message for players without bypass permission.",
                    "Supports Minecraft formatting codes with '&' prefix.")
                    .define("kick_message",
                            "&cThe server is full! &7Only VIP players can connect. | &cLe serveur est plein ! &7Seuls les joueurs VIP peuvent se connecter.");

            fakeMaxSlotsEnabled = builder.comment(
                    "Display 'max_slots' as the max player count in the server list.")
                    .define("fake_max_slots_enabled", true);

            hideJoinLeaveMessages = builder.comment(
                    "Hide vanilla 'Player joined/left' messages.")
                    .define("hide_join_leave_messages", true);

            builder.pop();
        }
    }

    static {
        Pair<Values, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Values::new);
        SPEC = specPair.getRight();
        VALUES = specPair.getLeft();
    }
}
