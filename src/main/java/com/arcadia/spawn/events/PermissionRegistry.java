package com.arcadia.spawn.events;

import com.arcadia.spawn.ArcadiaSpawnMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Centralized permission nodes for Arcadia Spawn admin commands.
 * Falls back to vanilla op-level when no permission backend (LuckPerms) is present.
 *
 * Default op-level fallback: 2 for most admin commands, 4 for dimension create/delete (destructive).
 */
@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID)
public final class PermissionRegistry {

    private PermissionRegistry() {}

    public static final PermissionNode<Boolean> CMD_RELOAD = node("command.reload", 2);
    public static final PermissionNode<Boolean> CMD_SETLOBBYTP = node("command.setlobbytp", 2);
    public static final PermissionNode<Boolean> CMD_DELLOBBYTP = node("command.dellobbytp", 2);
    public static final PermissionNode<Boolean> CMD_EDIT = node("command.edit", 2);
    public static final PermissionNode<Boolean> CMD_TP = node("command.tp", 2);
    public static final PermissionNode<Boolean> CMD_SETSPAWN = node("command.setspawn", 2);
    public static final PermissionNode<Boolean> CMD_DEBUG = node("command.debug", 2);
    public static final PermissionNode<Boolean> CMD_DIM_CREATE = node("command.dimension.create", 4);
    public static final PermissionNode<Boolean> CMD_DIM_DELETE = node("command.dimension.delete", 4);
    public static final PermissionNode<Boolean> CMD_DIM_LIST = node("command.dimension.list", 2);

    private static PermissionNode<Boolean> node(String path, int opLevel) {
        return new PermissionNode<>(
                ArcadiaSpawnMod.MOD_ID, path,
                PermissionTypes.BOOLEAN,
                (player, uuid, contexts) -> player != null && player.hasPermissions(opLevel)
        );
    }

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                CMD_RELOAD, CMD_SETLOBBYTP, CMD_DELLOBBYTP, CMD_EDIT,
                CMD_TP, CMD_SETSPAWN, CMD_DEBUG,
                CMD_DIM_CREATE, CMD_DIM_DELETE, CMD_DIM_LIST
        );
        ArcadiaSpawnMod.LOGGER.info("Arcadia Spawn permission nodes registered ({} nodes).", 10);
    }

    /**
     * Brigadier .requires() predicate. Console always passes.
     * Player passes if PermissionAPI returns true OR op-level fallback inside the node.
     */
    public static java.util.function.Predicate<CommandSourceStack> require(PermissionNode<Boolean> node, int opFallback) {
        return source -> {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                return source.hasPermission(opFallback);
            }
            try {
                return PermissionAPI.getPermission(player, node);
            } catch (Exception e) {
                return player.hasPermissions(opFallback);
            }
        };
    }
}
