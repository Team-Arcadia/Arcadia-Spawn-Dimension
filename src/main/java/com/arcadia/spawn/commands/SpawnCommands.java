package com.arcadia.spawn.commands;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.events.PermissionRegistry;
import com.arcadia.spawn.lobby.LobbyLocation;
import com.arcadia.spawn.lobby.LobbyManager;
import com.arcadia.spawn.lobby.LobbyMenu;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.tablist.CrossServerDb;
import com.arcadia.spawn.tablist.TabListConfig;
import com.arcadia.spawn.tablist.TabListManager;
import com.arcadia.spawn.util.InputValidation;
import com.arcadia.spawn.util.RateLimiter;
import com.arcadia.spawn.world.CustomDimensionDef;
import com.arcadia.spawn.world.CustomDimensionManager;
import com.arcadia.spawn.world.SpawnData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.Vec3;

public class SpawnCommands {

    private static final SuggestionProvider<CommandSourceStack> LOBBY_NAMES =
            (ctx, builder) -> {
                LobbyManager.getLocations().forEach(loc -> builder.suggest(loc.name()));
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> CUSTOM_DIM_IDS =
            (ctx, builder) -> {
                CustomDimensionManager.list().forEach(d -> builder.suggest(d.id));
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> DIM_PRESETS =
            (ctx, builder) -> {
                builder.suggest("flat");
                builder.suggest("void");
                builder.suggest("lobby");
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        // ── /arcadia_spawn (admin root) ─────────────────────────────────────
        dispatcher.register(Commands.literal("arcadia_spawn")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("reload")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_RELOAD, 2))
                        .executes(SpawnCommands::reloadConfig))

                .then(Commands.literal("setlobbytp")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_SETLOBBYTP, 2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> setLobbyTp(ctx, "minecraft:paper", ""))
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> setLobbyTp(ctx, getItemId(ctx, "item"), ""))
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(ctx -> setLobbyTp(ctx, getItemId(ctx, "item"),
                                                        StringArgumentType.getString(ctx, "description")))))))

                .then(Commands.literal("dellobbytp")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_DELLOBBYTP, 2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(LOBBY_NAMES)
                                .executes(SpawnCommands::deleteLobbyTp)))

                .then(Commands.literal("edit")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_EDIT, 2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(LOBBY_NAMES)
                                .then(Commands.literal("description")
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(SpawnCommands::editLobbyDescription)))
                                .then(Commands.literal("item")
                                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                                .executes(SpawnCommands::editLobbyItem)))
                                .then(Commands.literal("location")
                                        .executes(SpawnCommands::editLobbyLocation))))

                .then(Commands.literal("tp")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_TP, 2))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests(LOBBY_NAMES)
                                .executes(SpawnCommands::tpLobby)))

                .then(Commands.literal("setspawn")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_SETSPAWN, 2))
                        .executes(SpawnCommands::setSpawn))

                // ── dimension subcommands (NEW in 1.5.3) ──
                .then(Commands.literal("dimension")
                        .then(Commands.literal("create")
                                .requires(PermissionRegistry.require(PermissionRegistry.CMD_DIM_CREATE, 4))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> createDimension(ctx, "flat", null))
                                        .then(Commands.argument("preset", StringArgumentType.word())
                                                .suggests(DIM_PRESETS)
                                                .executes(ctx -> createDimension(ctx,
                                                        StringArgumentType.getString(ctx, "preset"), null))
                                                .then(Commands.argument("biome", StringArgumentType.greedyString())
                                                        .executes(ctx -> createDimension(ctx,
                                                                StringArgumentType.getString(ctx, "preset"),
                                                                StringArgumentType.getString(ctx, "biome")))))))
                        .then(Commands.literal("delete")
                                .requires(PermissionRegistry.require(PermissionRegistry.CMD_DIM_DELETE, 4))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(CUSTOM_DIM_IDS)
                                        .executes(ctx -> deleteDimension(ctx, false))
                                        .then(Commands.argument("purge", BoolArgumentType.bool())
                                                .executes(ctx -> deleteDimension(ctx, BoolArgumentType.getBool(ctx, "purge"))))))
                        .then(Commands.literal("list")
                                .requires(PermissionRegistry.require(PermissionRegistry.CMD_DIM_LIST, 2))
                                .executes(SpawnCommands::listDimensions)))

                // ── tablist subcommands (NEW in 1.5.3) ──
                .then(Commands.literal("tablist")
                        .requires(PermissionRegistry.require(PermissionRegistry.CMD_TABLIST, 2))
                        .then(Commands.literal("reload").executes(SpawnCommands::tablistReload))
                        .then(Commands.literal("status").executes(SpawnCommands::tablistStatus))
                        .then(Commands.literal("peers").executes(SpawnCommands::tablistPeers)))
        );

        dispatcher.register(Commands.literal("lobby")
                .executes(SpawnCommands::openLobbyMenu));

        dispatcher.register(Commands.literal("spawn")
                .executes(SpawnCommands::teleportToSpawn));

        dispatcher.register(Commands.literal("setlobbyspawn")
                .requires(PermissionRegistry.require(PermissionRegistry.CMD_SETSPAWN, 2))
                .executes(SpawnCommands::setSpawn));

        RTPCommand.register(dispatcher);
    }

    public static void openLobbyForPlayer(ServerPlayer player) {
        if (!RateLimiter.tryAcquire(player.getUUID(), "open_lobby_menu", 5, 10_000L)) {
            // silently throttle — prevents GUI spam from packet abuse
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new LobbyMenu(id, inv),
                LocalizationManager.getComponent(player, "arcadia_spawn.menu.title")));
    }

    private static String getItemId(CommandContext<CommandSourceStack> context, String argName) {
        return BuiltInRegistries.ITEM.getKey(
                ItemArgument.getItem(context, argName).getItem()).toString();
    }

    // ── Lobby TP ────────────────────────────────────────────────────────────

    private static int setLobbyTp(CommandContext<CommandSourceStack> context, String item, String description) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(context, "name");
        if (!InputValidation.isValidLobbyName(name)) {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.invalid_name")));
            return 0;
        }

        String safeDesc = InputValidation.sanitizeDescription(description);

        LobbyLocation loc = LobbyLocation.of(name,
                player.level().dimension().location().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                safeDesc, item);

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
        }
        player.sendSystemMessage(ArcadiaMessages.error(
                LocalizationManager.getString(player, "arcadia_spawn.command.dellobby.fail", name)));
        return 0;
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
            String desc = InputValidation.sanitizeDescription(StringArgumentType.getString(ctx, "description"));
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

    // ── Reload ──────────────────────────────────────────────────────────────

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        LobbyManager.reload();
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(ArcadiaMessages.success(
                    LocalizationManager.getString(player, "arcadia_spawn.command.reload.success")));
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Configuration reloaded."), true);
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

    public static int setSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String dimensionId = player.level().dimension().location().toString();
        SpawnData data = SpawnData.get();
        data.setSpawn(player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), dimensionId);

        player.sendSystemMessage(ArcadiaMessages.success(
                LocalizationManager.getString(player, "arcadia_spawn.command.setspawn.success")));
        return 1;
    }

    // ── Dimension management ────────────────────────────────────────────────

    private static int createDimension(CommandContext<CommandSourceStack> ctx, String preset, String biome) {
        String id = StringArgumentType.getString(ctx, "id");
        CommandSourceStack source = ctx.getSource();

        if (!InputValidation.isValidDimensionId(id)) {
            source.sendFailure(Component.literal("Invalid id. Use lowercase alphanumeric + underscore, 3-32 chars.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (CustomDimensionManager.exists(id)) {
            source.sendFailure(Component.literal("Dimension '" + id + "' already exists.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // A biome that does not exist makes the generated level stem unparseable, and the
        // whole world then refuses to load on the next start. Reject it here, while the
        // admin is still at the console, rather than at boot.
        if (biome != null && !biome.isBlank() && !biomeExists(source, biome)) {
            source.sendFailure(Component.literal("Unknown biome: " + biome).withStyle(ChatFormatting.RED));
            return 0;
        }

        if (CustomDimensionManager.create(id, preset, biome)) {
            source.sendSuccess(() -> Component.literal("Created dimension '" + CustomDimensionManager.CUSTOM_NAMESPACE + ":" + id +
                    "'. RESTART THE SERVER for it to load.")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        source.sendFailure(Component.literal("Failed to create dimension. See logs.").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static boolean biomeExists(CommandSourceStack source, String biome) {
        net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.tryParse(biome);
        if (loc == null) return false;
        return source.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .containsKey(loc);
    }

    private static int deleteDimension(CommandContext<CommandSourceStack> ctx, boolean purge) {
        String id = StringArgumentType.getString(ctx, "id");
        CommandSourceStack source = ctx.getSource();

        // Validate the id before it reaches CustomDimensionManager.exists()/delete(), which
        // resolve it into a filesystem path (id + ".json"). Without this, a crafted id with
        // ".." or "/" components would escape the dimensions directory (path traversal).
        // createDimension() already validates; delete must match.
        if (!InputValidation.isValidDimensionId(id)) {
            source.sendFailure(Component.literal("Invalid id. Use lowercase alphanumeric + underscore, 3-32 chars.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!CustomDimensionManager.exists(id)) {
            source.sendFailure(Component.literal("No such dimension: " + id).withStyle(ChatFormatting.RED));
            return 0;
        }

        if (CustomDimensionManager.delete(id, purge)) {
            String msg = "Deleted dimension '" + CustomDimensionManager.CUSTOM_NAMESPACE + ":" + id +
                    "'. It stays loaded until the server restarts." + (purge ?
                    " Its world data under world/dimensions/" + CustomDimensionManager.CUSTOM_NAMESPACE + "/" + id +
                            " is deleted automatically when the server stops."
                    : " World data preserved.");
            source.sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        source.sendFailure(Component.literal("Failed to delete dimension. See logs.").withStyle(ChatFormatting.RED));
        return 0;
    }

    // ── TabList admin ───────────────────────────────────────────────────────

    private static int tablistReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        // Re-apply to all currently online players. Config is reloaded automatically
        // by NeoForge when the toml file is edited; this just forces the refresh tick.
        TabListManager.tick(source.getServer());
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            TabListManager.onPlayerJoin(player);
        }
        source.sendSuccess(() -> Component.literal("TabList refreshed for " +
                source.getServer().getPlayerList().getPlayerCount() + " player(s).")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int tablistStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("─── TabList Status ───")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("  Enabled: " + TabListConfig.VALUES.enabled.get())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  Group sorting: " + TabListConfig.VALUES.groupSortingEnabled.get())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  LuckPerms detected: " + com.arcadia.spawn.tablist.GradeResolver.hasLuckPerms())
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  Cross-server: " + TabListConfig.VALUES.crossServerEnabled.get() +
                " (DB " + (CrossServerDb.isAvailable() ? "available" : "not available") + ")")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("  This server id: " + CrossServerDb.localServerId())
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int tablistPeers(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!CrossServerDb.isAvailable()) {
            source.sendFailure(Component.literal("arcadia-lib DB is not active. Enable it in arcadia-lib config.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        long timeoutMs = TabListConfig.VALUES.peerTimeoutSeconds.get() * 1000L;
        CrossServerDb.fetchPeers(timeoutMs).thenAccept(peers -> source.getServer().execute(() -> {
            source.sendSuccess(() -> Component.literal("─── Peer Servers (" + peers.size() + ") ───")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            if (peers.isEmpty()) {
                source.sendSuccess(() -> Component.literal("  (no rows in shared DB yet)")
                        .withStyle(ChatFormatting.GRAY), false);
                return;
            }
            for (CrossServerDb.PeerSnapshot p : peers) {
                String tag = p.alive() ? "[ALIVE]" : "[STALE]";
                ChatFormatting color = p.alive() ? ChatFormatting.GREEN : ChatFormatting.RED;
                source.sendSuccess(() -> Component.literal(String.format(
                        "  %s %-14s %d/%d  display=%s",
                        tag, p.serverId(), p.online(), p.max(),
                        (p.displayName() == null || p.displayName().isBlank()) ? "-" : p.displayName()))
                        .withStyle(color), false);
            }
        }));
        return 1;
    }

    private static int listDimensions(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        java.util.Collection<CustomDimensionDef> defs = CustomDimensionManager.list();

        source.sendSuccess(() -> Component.literal("Custom dimensions (" + defs.size() + "):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        if (defs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("  (none — use /arcadia_spawn dimension create <id>)")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        for (CustomDimensionDef def : defs) {
            boolean loaded = source.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            CustomDimensionManager.CUSTOM_NAMESPACE, def.id))) != null;
            source.sendSuccess(() -> Component.literal("  • " + CustomDimensionManager.CUSTOM_NAMESPACE + ":" + def.id +
                    (loaded ? " [LOADED]" : " [pending restart]"))
                    .withStyle(loaded ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        }
        return 1;
    }
}
