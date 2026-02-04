package com.vyrriox.arcadiaspawn.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.vyrriox.arcadiaspawn.lobby.LobbyLocation;
import com.vyrriox.arcadiaspawn.lobby.LobbyManager;
import com.vyrriox.arcadiaspawn.lobby.LobbyMenu;
import com.vyrriox.arcadiaspawn.lobby.LocalizationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import net.minecraft.commands.CommandBuildContext;

public class ModCommands {

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                        CommandBuildContext buildContext) {

                // Main command: /arcadialobby
                dispatcher.register(Commands.literal("arcadialobby")
                                .requires(source -> source.hasPermission(2)) // Base permission for admin commands
                                                                             // (except maybe tp?)

                                // reload
                                .then(Commands.literal("reload")
                                                .executes(ModCommands::reloadConfig))

                                // setlobbytp <name> [item] [description]
                                .then(Commands.literal("setlobbytp")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                                .executes(ctx -> setLobbyTp(ctx, "minecraft:paper", ""))
                                                                .then(Commands.argument("item",
                                                                                ItemArgument.item(buildContext))
                                                                                .executes(ctx -> setLobbyTp(ctx,
                                                                                                getItemId(ctx, "item"),
                                                                                                ""))
                                                                                .then(Commands.argument("description",
                                                                                                StringArgumentType
                                                                                                                .greedyString())
                                                                                                .executes(ctx -> setLobbyTp(
                                                                                                                ctx,
                                                                                                                getItemId(ctx, "item"),
                                                                                                                StringArgumentType
                                                                                                                                .getString(ctx, "description")))))))

                                // dellobbytp <name>
                                .then(Commands.literal("dellobbytp")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                                .suggests((ctx, builder) -> {
                                                                        LobbyManager.getLocations()
                                                                                        .forEach(loc -> builder.suggest(
                                                                                                        loc.name()));
                                                                        return builder.buildFuture();
                                                                })
                                                                .executes(ModCommands::deleteLobbyTp)))

                                // edit <name> ...
                                .then(Commands.literal("edit")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                                .suggests((ctx, builder) -> {
                                                                        LobbyManager.getLocations()
                                                                                        .forEach(loc -> builder.suggest(
                                                                                                        loc.name()));
                                                                        return builder.buildFuture();
                                                                })
                                                                // edit <name> description <text>
                                                                .then(Commands.literal("description")
                                                                                .then(Commands.argument("description",
                                                                                                StringArgumentType
                                                                                                                .greedyString())
                                                                                                .executes(ModCommands::editLobbyDescription)))
                                                                // edit <name> item <item>
                                                                .then(Commands.literal("item")
                                                                                .then(Commands.argument("item",
                                                                                                ItemArgument.item(
                                                                                                                buildContext))
                                                                                                .executes(ModCommands::editLobbyItem)))
                                                                // edit <name> location (set to current)
                                                                .then(Commands.literal("location")
                                                                                .executes(ModCommands::editLobbyLocation))))

                                // tp <name>
                                .then(Commands.literal("tp")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                                .suggests((ctx, builder) -> {
                                                                        LobbyManager.getLocations()
                                                                                        .forEach(loc -> builder.suggest(
                                                                                                        loc.name()));
                                                                        return builder.buildFuture();
                                                                })
                                                                .executes(ModCommands::tpLobby))));

                // Command /lobby (Opens Menu) - unchanged
                dispatcher.register(Commands.literal("lobby")
                                .executes(ModCommands::openLobbyMenu));

                // Alias /spawn (Keep as is) - unchanged
                dispatcher.register(Commands.literal("spawn")
                                .executes(ModCommands::teleportToSpawn));

                // Command /arcadiartp - unchanged
                RTPCommand.register(dispatcher);
        }

        private static String getItemId(CommandContext<CommandSourceStack> context, String argName) {
                net.minecraft.world.item.Item item = ItemArgument.getItem(context, argName).getItem();
                return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        }

        // --- Implementation Methods ---

        private static int setLobbyTp(CommandContext<CommandSourceStack> context, String item, String description) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                String name = StringArgumentType.getString(context, "name");
                LobbyLocation loc = LobbyLocation.of(name,
                                player.level().dimension().location().toString(),
                                player.getX(), player.getY(), player.getZ(),
                                player.getYRot(), player.getXRot(),
                                description, item);

                LobbyManager.addLocation(loc);

                context.getSource().sendSuccess(() -> LocalizationManager
                                .getComponent(player, "arcadiaspawn.command.setlobby.success", name)
                                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                return 1;
        }

        private static int deleteLobbyTp(CommandContext<CommandSourceStack> context) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                String name = StringArgumentType.getString(context, "name");
                // We simply call removeLocation(name) now
                boolean removed = LobbyManager.removeLocation(name);

                if (removed) {
                        context.getSource().sendSuccess(() -> LocalizationManager
                                        .getComponent(player, "arcadiaspawn.command.dellobby.success", name)
                                        .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                        return 1;
                } else {
                        context.getSource().sendFailure(
                                        LocalizationManager.getComponent(player, "arcadiaspawn.command.dellobby.fail",
                                                        name));
                        return 0;
                }
        }

        private static int tpLobby(CommandContext<CommandSourceStack> context) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                String name = StringArgumentType.getString(context, "name");
                LobbyLocation loc = LobbyManager.getLocation(name);

                if (loc == null) {
                        context.getSource().sendFailure(Component.literal("Lobby not found: " + name)); // Fallback or
                                                                                                        // needs
                                                                                                        // localization
                                                                                                        // key
                        return 0;
                }

                net.minecraft.server.level.ServerLevel level = context.getSource().getServer()
                                .getLevel(loc.dimension());
                if (level == null) {
                        context.getSource().sendFailure(Component.literal("Dimension not found for lobby: " + name));
                        return 0;
                }

                player.teleportTo(level, loc.x(), loc.y(), loc.z(), java.util.Set.of(), loc.yaw(), loc.pitch());
                player.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

                context.getSource().sendSuccess(() -> Component.literal("Teleported to lobby: " + name)
                                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                return 1;
        }

        private static int editLobbyDescription(CommandContext<CommandSourceStack> context) {
                return editLobby(context, (loc, ctx) -> {
                        String desc = StringArgumentType.getString(ctx, "description");
                        return new LobbyLocation(loc.name(), loc.dimension(), loc.x(), loc.y(), loc.z(), loc.yaw(),
                                        loc.pitch(), desc, loc.item());
                });
        }

        private static int editLobbyItem(CommandContext<CommandSourceStack> context) {
                return editLobby(context, (loc, ctx) -> {
                        String item = getItemId(ctx, "item");
                        return new LobbyLocation(loc.name(), loc.dimension(), loc.x(), loc.y(), loc.z(), loc.yaw(),
                                        loc.pitch(), loc.description(), item);
                });
        }

        private static int editLobbyLocation(CommandContext<CommandSourceStack> context) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                return editLobby(context, (loc, ctx) -> {
                        return LobbyLocation.of(loc.name(),
                                        player.level().dimension().location().toString(),
                                        player.getX(), player.getY(), player.getZ(),
                                        player.getYRot(), player.getXRot(),
                                        loc.description(), loc.item());
                });
        }

        private interface LobbyEditor {
                LobbyLocation edit(LobbyLocation old, CommandContext<CommandSourceStack> ctx);
        }

        private static int editLobby(CommandContext<CommandSourceStack> context, LobbyEditor editor) {
                String name = StringArgumentType.getString(context, "name");
                LobbyLocation loc = LobbyManager.getLocation(name);

                if (loc == null) {
                        context.getSource().sendFailure(Component.literal("Lobby not found: " + name));
                        return 0;
                }

                LobbyLocation newLoc = editor.edit(loc, context);
                LobbyManager.updateLocation(name, newLoc);

                context.getSource().sendSuccess(() -> Component.literal("Lobby '" + name + "' updated.")
                                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                return 1;
        }

        private static int openLobbyMenu(CommandContext<CommandSourceStack> context) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                player.openMenu(new SimpleMenuProvider(
                                (id, inv, p) -> new LobbyMenu(id, inv),
                                LocalizationManager.getComponent(player, "arcadiaspawn.menu.title")));

                return 1;
        }

        private static int reloadConfig(CommandContext<CommandSourceStack> context) {
                LobbyManager.reload();
                ServerPlayer player = context.getSource().getPlayer();
                if (player != null) {
                        context.getSource().sendSuccess(() -> LocalizationManager
                                        .getComponent(player, "arcadiaspawn.command.reload.success")
                                        .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                } else {
                        // Console feedback
                        context.getSource().sendSuccess(() -> Component.literal("Configuration reloaded."), true);
                }
                return 1;
        }

        // Existing /spawn logic
        public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> SPAWN_LEVEL_KEY = net.minecraft.resources.ResourceKey
                        .create(net.minecraft.core.registries.Registries.DIMENSION,
                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("arcadia",
                                                        "spawn"));

        private static int teleportToSpawn(CommandContext<CommandSourceStack> context) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                net.minecraft.server.level.ServerLevel spawnLevel = context.getSource().getServer()
                                .getLevel(SPAWN_LEVEL_KEY);

                if (spawnLevel == null) {
                        context.getSource().sendFailure(
                                        LocalizationManager.getComponent(player,
                                                        "arcadiaspawn.command.spawn.fail_dim"));
                        return 0;
                }

                com.vyrriox.arcadiaspawn.world.SpawnData data = com.vyrriox.arcadiaspawn.world.SpawnData
                                .get(spawnLevel);
                if (!data.isSet()) {
                        context.getSource().sendFailure(LocalizationManager.getComponent(player,
                                        "arcadiaspawn.command.spawn.fail_unset"));
                        return 0;
                }

                player.resetFallDistance();
                player.teleportTo(spawnLevel, data.getX(), data.getY(), data.getZ(), java.util.Set.of(), data.getYaw(),
                                data.getPitch());
                player.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

                return 1;
        }

        public static int setSpawn(CommandContext<CommandSourceStack> context) {
                ServerPlayer player = context.getSource().getPlayer();
                if (player == null)
                        return 0;

                com.vyrriox.arcadiaspawn.world.SpawnData data = com.vyrriox.arcadiaspawn.world.SpawnData
                                .get(context.getSource().getLevel());
                data.setSpawn(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

                context.getSource().sendSuccess(
                                () -> LocalizationManager.getComponent(player, "arcadiaspawn.command.setspawn.success")
                                                .withStyle(net.minecraft.ChatFormatting.GREEN),
                                true);
                return 1;
        }
}
