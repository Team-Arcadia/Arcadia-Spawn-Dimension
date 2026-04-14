package com.arcadia.spawn.commands;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.spawn.commands.TeleportHelper;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.events.ModEvents;
import com.arcadia.spawn.lobby.LobbyLocation;
import com.arcadia.spawn.lobby.LobbyManager;
import com.arcadia.spawn.lobby.LobbyMenu;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.world.SpawnData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class SpawnCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        // ── /arcadia_spawn (admin root) ─────────────────────────────────────
        dispatcher.register(Commands.literal("arcadia_spawn")
                .requires(source -> source.hasPermission(2))

                // reload
                .then(Commands.literal("reload")
                        .executes(SpawnCommands::reloadConfig))

                // setlobbytp <name> [item] [description]
                .then(Commands.literal("setlobbytp")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> setLobbyTp(ctx, "minecraft:paper", ""))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> setLobbyTp(ctx, getItemId(ctx, "item"), ""))
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(ctx -> setLobbyTp(ctx, getItemId(ctx, "item"),
                                                        StringArgumentType.getString(ctx, "description")))))))

                // dellobbytp <name>
                .then(Commands.literal("dellobbytp")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    LobbyManager.getLocations().forEach(loc -> builder.suggest(loc.name()));
                                    return builder.buildFuture();
                                })
                                .executes(SpawnCommands::deleteLobbyTp)))

                // edit <name> ...
                .then(Commands.literal("edit")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    LobbyManager.getLocations().forEach(loc -> builder.suggest(loc.name()));
                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("description")
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(SpawnCommands::editLobbyDescription)))
                                .then(Commands.literal("item")
                                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                .executes(SpawnCommands::editLobbyItem)))
                                .then(Commands.literal("location")
                                        .executes(SpawnCommands::editLobbyLocation))))

                // tp <name>
                .then(Commands.literal("tp")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    LobbyManager.getLocations().forEach(loc -> builder.suggest(loc.name()));
                                    return builder.buildFuture();
                                })
                                .executes(SpawnCommands::tpLobby)))

                // setspawn
                .then(Commands.literal("setspawn")
                        .executes(SpawnCommands::setSpawn))
        );

        // ── /lobby (opens GUI menu) ─────────────────────────────────────────
        dispatcher.register(Commands.literal("lobby")
                .executes(SpawnCommands::openLobbyMenu));

        // ── /spawn (teleport to spawn dimension) ────────────────────────────
        dispatcher.register(Commands.literal("spawn")
                .executes(SpawnCommands::teleportToSpawn));

        // ── /setlobbyspawn (alias for backward compat) ──────────────────────
        dispatcher.register(Commands.literal("setlobbyspawn")
                .requires(source -> source.hasPermission(2))
                .executes(SpawnCommands::setSpawn));

        // ── /arcadiartp ──────────────────────────────────────────────────────
        RTPCommand.register(dispatcher);
    }

    // ── Public helper for ArcadiaModRegistry action ─────────────────────────
    public static void openLobbyForPlayer(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new LobbyMenu(id, inv),
                LocalizationManager.getComponent(player, "arcadia_spawn.menu.title")));
    }

    // ── Item ID resolver ────────────────────────────────────────────────────
    private static String getItemId(CommandContext<CommandSourceStack> context, String argName) {
        return BuiltInRegistries.ITEM.getKey(
                ItemArgument.getItem(context, argName).getItem()).toString();
    }

    // ── Lobby TP management ─────────────────────────────────────────────────

    private static int setLobbyTp(CommandContext<CommandSourceStack> context, String item, String description) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(context, "name");
        LobbyLocation loc = LobbyLocation.of(name,
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                description, item);

        LobbyManager.addLocation(loc);
        player.sendSystemMessage(ArcadiaMessages.success(
                LocalizationManager.getString(player, "arcadia_spawn.command.setlobby.success", name)));
        return 1;
    }

    private static int deleteLobbyTp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(context, "name");
        if (LobbyManager.removeLocation(name)) {
            player.sendSystemMessage(ArcadiaMessages.success(
                    LocalizationManager.getString(player, "arcadia_spawn.command.dellobby.success", name)));
            return 1;
        } else {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.dellobby.fail", name)));
            return 0;
        }
    }

    private static int tpLobby(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(context, "name");
        LobbyLocation loc = LobbyManager.getLocation(name);

        if (loc == null) {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.dellobby.fail", name)));
            return 0;
        }

        ServerLevel level = context.getSource().getServer().getLevel(loc.dimension());
        if (level == null) {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.teleport.fail")));
            return 0;
        }

        TeleportHelper.teleportNow(player, new Vec3(loc.x(), loc.y(), loc.z()), level);
        player.sendSystemMessage(ArcadiaMessages.success(
                LocalizationManager.getString(player, "arcadia_spawn.teleport.success", name)));
        return 1;
    }

    // ── Edit helpers ────────────────────────────────────────────────────────

    private interface LobbyEditor {
        LobbyLocation edit(LobbyLocation old, CommandContext<CommandSourceStack> ctx);
    }

    private static int editLobby(CommandContext<CommandSourceStack> context, LobbyEditor editor) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(context, "name");
        LobbyLocation loc = LobbyManager.getLocation(name);
        if (loc == null) {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.dellobby.fail", name)));
            return 0;
        }

        LobbyManager.updateLocation(name, editor.edit(loc, context));
        player.sendSystemMessage(ArcadiaMessages.success(
                LocalizationManager.getString(player, "arcadia_spawn.command.edit.success", name)));
        return 1;
    }

    private static int editLobbyDescription(CommandContext<CommandSourceStack> context) {
        return editLobby(context, (loc, ctx) -> {
            String desc = StringArgumentType.getString(ctx, "description");
            return new LobbyLocation(loc.name(), loc.dimension(), loc.x(), loc.y(), loc.z(),
                    loc.yaw(), loc.pitch(), desc, loc.item());
        });
    }

    private static int editLobbyItem(CommandContext<CommandSourceStack> context) {
        return editLobby(context, (loc, ctx) -> {
            String item = getItemId(ctx, "item");
            return new LobbyLocation(loc.name(), loc.dimension(), loc.x(), loc.y(), loc.z(),
                    loc.yaw(), loc.pitch(), loc.description(), item);
        });
    }

    private static int editLobbyLocation(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        return editLobby(context, (loc, ctx) -> LobbyLocation.of(loc.name(),
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                loc.description(), loc.item()));
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    private static int openLobbyMenu(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        openLobbyForPlayer(player);
        return 1;
    }

    // ── Config reload ───────────────────────────────────────────────────────

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        LobbyManager.reload();
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(ArcadiaMessages.success(
                    LocalizationManager.getString(player, "arcadia_spawn.command.reload.success")));
        } else {
            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Configuration reloaded."), true);
        }
        return 1;
    }

    // ── Spawn TP ────────────────────────────────────────────────────────────

    private static int teleportToSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        SpawnData data = SpawnData.get();
        if (!data.isSet()) {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.spawn.fail_unset")));
            return 0;
        }

        // Use the dimension stored in SpawnData (not hardcoded arcadia:spawn)
        ServerLevel targetLevel = context.getSource().getServer().getLevel(data.getDimensionKey());
        if (targetLevel == null) {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.spawn.fail_dim")));
            return 0;
        }

        int warmup = SpawnConfig.COMMON.spawnTpWarmupTicks.get();
        int cooldownMs = SpawnConfig.COMMON.spawnTpCooldownSeconds.get() * 1000;

        player.resetFallDistance();

        TeleportHelper.teleportWithWarmup(player,
                new Vec3(data.getX(), data.getY(), data.getZ()),
                targetLevel, warmup, cooldownMs, "spawn_tp");
        return 1;
    }

    // ── Set Spawn ───────────────────────────────────────────────────────────

    public static int setSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        // Store spawn with the current dimension — fixes wrong-dimension teleport bug
        String dimensionId = player.level().dimension().location().toString();
        SpawnData data = SpawnData.get();
        data.setSpawn(player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), dimensionId);

        player.sendSystemMessage(ArcadiaMessages.success(
                LocalizationManager.getString(player, "arcadia_spawn.command.setspawn.success")));
        return 1;
    }
}
