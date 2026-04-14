package com.arcadia.spawn.events;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.config.SlotBypassConfig;
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
 * Slot bypass using NeoForge PermissionAPI (LuckPerms compatible).
 * Permission node: arcadia_spawn.slots.bypass
 */
@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID)
public class SlotBypassHandler {

    public static final PermissionNode<Boolean> SLOT_BYPASS_NODE = new PermissionNode<>(
            ArcadiaSpawnMod.MOD_ID, "slots.bypass",
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> false
    );

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(SLOT_BYPASS_NODE);
        ArcadiaSpawnMod.LOGGER.info("Registered permission node: {}", SLOT_BYPASS_NODE.getNodeName());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SlotBypassConfig.VALUES.enabled.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        int maxSlots = SlotBypassConfig.VALUES.maxSlots.get();
        int currentPlayers = server.getPlayerList().getPlayerCount();

        if (currentPlayers <= maxSlots) return;

        try {
            boolean hasBypass = PermissionAPI.getPermission(player, SLOT_BYPASS_NODE);
            if (hasBypass) {
                ArcadiaSpawnMod.LOGGER.info("Player {} bypassed slot limit.", player.getName().getString());
                return;
            }
        } catch (Exception e) {
            ArcadiaSpawnMod.LOGGER.error("Error checking slot bypass for {}", player.getName().getString(), e);
            return; // Fail-open
        }

        String rawMessage = SlotBypassConfig.VALUES.kickMessage.get();
        String formatted = rawMessage.replace("&", "\u00A7");
        player.connection.disconnect(Component.literal(formatted));
        ArcadiaSpawnMod.LOGGER.info("Kicked {} — server full ({}/{}).", player.getName().getString(), currentPlayers, maxSlots);
    }
}
