package com.vyrriox.arcadiaspawn.events;

import com.vyrriox.arcadiaspawn.ArcadiaSpawnMod;
import com.vyrriox.arcadiaspawn.config.SlotBypassConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * @author vyrriox
 * Handles slot bypass logic using NeoForge PermissionAPI.
 * Compatible with LuckPerms when installed (LuckPerms replaces the default permission handler).
 *
 * Permission node: arcadia_spawn.slots.bypass
 * Assign this permission in LuckPerms to allow specific groups to bypass slot limits.
 */
@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID)
public class SlotBypassHandler {

    // Permission node for slot bypass
    public static final PermissionNode<Boolean> SLOT_BYPASS_NODE = new PermissionNode<>(
            ArcadiaSpawnMod.MOD_ID, "slots.bypass",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> false // Default: no bypass
    );

    /**
     * Register permission nodes with NeoForge PermissionAPI.
     * This is required for LuckPerms integration.
     */
    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(SLOT_BYPASS_NODE);
        ArcadiaSpawnMod.LOGGER.info("Registered slot bypass permission node: {}", SLOT_BYPASS_NODE.getNodeName());
    }

    /**
     * Check slot limit when a player logs in.
     * If the server is full and the player lacks the bypass permission, disconnect them.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SlotBypassConfig.VALUES.enabled.get()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        int maxSlots = SlotBypassConfig.VALUES.maxSlots.get();
        // Count currently online players (excluding the joining player who is already counted)
        int currentPlayers = server.getPlayerList().getPlayerCount();

        // If below limit, no need to check permissions
        if (currentPlayers <= maxSlots) return;

        // Server is full — check bypass permission
        try {
            boolean hasBypass = PermissionAPI.getPermission(player, SLOT_BYPASS_NODE);
            if (hasBypass) {
                ArcadiaSpawnMod.LOGGER.info("Player {} bypassed slot limit via permission.", player.getName().getString());
                return;
            }
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Error checking slot bypass permission for {}", player.getName().getString(), e);
            // On error, allow the player to join (fail-open)
            return;
        }

        // No bypass permission — kick the player
        String rawMessage = SlotBypassConfig.VALUES.kickMessage.get();
        String formatted = rawMessage.replace("&", "\u00A7");
        player.connection.disconnect(Component.literal(formatted));
        ArcadiaSpawnMod.LOGGER.info("Kicked player {} — server full ({}/{}) and no bypass permission.",
                player.getName().getString(), currentPlayers, maxSlots);
    }
}
