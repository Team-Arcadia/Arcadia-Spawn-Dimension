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
        public final ModConfigSpec.ConfigValue<Boolean> fakeMaxSlotsEnabled;
        public final ModConfigSpec.ConfigValue<Boolean> hideJoinLeaveMessages;

        Values(ModConfigSpec.Builder builder) {
            builder.push("Slot Bypass");
            builder.comment("Configuration du systeme de bypass de slots.",
                    "Les joueurs avec la permission 'arcadia_spawn.slots.bypass' peuvent se connecter meme si le serveur est plein.",
                    "Assignez cette permission aux groupes/joueurs LuckPerms selon vos besoins.");

            enabled = builder.comment("Activer ou desactiver le systeme de bypass de slots.")
                    .define("enabled", false);

            maxSlots = builder.comment(
                    "Nombre maximum de joueurs avant que le bypass soit necessaire.",
                    "Les joueurs sans la permission de bypass seront kick quand le serveur atteint ce nombre.")
                    .defineInRange("max_slots", 20, 1, 1000);

            kickMessage = builder.comment(
                    "Le message de kick affiche aux joueurs sans permission de bypass quand le serveur est plein.",
                    "Supporte les codes de formatage Minecraft avec le prefixe '&'.")
                    .define("kick_message", "&cLe serveur est plein ! &7Seuls les joueurs VIP peuvent se connecter.");

            fakeMaxSlotsEnabled = builder.comment(
                    "Si active, le nombre 'max_slots' ci-dessus sera affiche dans la liste des serveurs comme le nombre maximum de joueurs.",
                    "Cela permet de donner l'impression que le serveur est plein, meme si la vraie limite est plus elevee.")
                    .define("fake_max_slots_enabled", true);

            hideJoinLeaveMessages = builder.comment(
                    "Si active, les messages natifs 'Joueur a rejoint la partie' et 'Joueur a quitte la partie' seront masques.",
                    "Utile pour eviter le spam dans le chat lorsqu'il y a beaucoup de connexions/deconnexions.")
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
