package com.arcadia.spawn.commands;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.config.SlotBypassConfig;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.data.RTPData;
import com.arcadia.spawn.events.ModEvents;
import com.arcadia.spawn.lobby.LobbyLocation;
import com.arcadia.spawn.lobby.LobbyManager;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.registry.AttachmentRegistry;
import com.arcadia.spawn.world.SpawnData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Debug and diagnostic commands for Arcadia Spawn.
 * All under /arcadia_spawn debug (requires op level 2).
 */
public class DebugCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arcadia_spawn")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")

                        // ── Status overview ──
                        .then(Commands.literal("status")
                                .executes(DebugCommands::showStatus))

                        // ── Spawn info ──
                        .then(Commands.literal("spawn")
                                .executes(DebugCommands::showSpawnInfo))

                        // ── Dimension info ──
                        .then(Commands.literal("dimension")
                                .executes(DebugCommands::showDimensionInfo))

                        // ── Lobby list ──
                        .then(Commands.literal("lobbies")
                                .executes(DebugCommands::listLobbies))

                        // ── Player RTP data ──
                        .then(Commands.literal("rtp")
                                .executes(DebugCommands::showRtpData)
                                .then(Commands.literal("reset")
                                        .executes(DebugCommands::resetRtpData)))

                        // ── Config dump ──
                        .then(Commands.literal("config")
                                .executes(DebugCommands::dumpConfig))

                        // ── Slot bypass info ──
                        .then(Commands.literal("slots")
                                .executes(DebugCommands::showSlotInfo))

                        // ── Player info ──
                        .then(Commands.literal("player")
                                .executes(DebugCommands::showPlayerInfo))

                        // ── Force reload everything ──
                        .then(Commands.literal("reload_all")
                                .executes(DebugCommands::forceReloadAll))

                        // ── Force first-join tag reset ──
                        .then(Commands.literal("reset_visited")
                                .executes(DebugCommands::resetVisitedTag))

                        // ── Teleport metrics ──
                        .then(Commands.literal("tps")
                                .executes(DebugCommands::showTps))

                        // ── Language check ──
                        .then(Commands.literal("lang")
                                .executes(DebugCommands::showLangInfo)
                                .then(Commands.argument("key", StringArgumentType.greedyString())
                                        .executes(DebugCommands::testLangKey)))
                ));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void send(CommandSourceStack source, String msg) {
        source.sendSuccess(() -> Component.literal(msg), false);
    }

    private static void sendHeader(CommandSourceStack source, String title) {
        source.sendSuccess(() -> Component.literal("═══ " + title + " ═══")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    private static void sendKV(CommandSourceStack source, String key, Object value) {
        source.sendSuccess(() -> Component.literal("  " + key + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(value))
                        .withStyle(ChatFormatting.WHITE)), false);
    }

    // ── Commands ────────────────────────────────────────────────────────────

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        sendHeader(source, "Arcadia Spawn — Status");
        sendKV(source, "Mod ID", ArcadiaSpawnMod.MOD_ID);
        sendKV(source, "Lobby Locations", LobbyManager.getLocationCount());

        SpawnData data = SpawnData.get();
        sendKV(source, "Spawn Set", data.isSet());
        if (data.isSet()) {
            sendKV(source, "Spawn Dimension", data.getDimensionId());
            sendKV(source, "Spawn Pos", String.format("%.1f, %.1f, %.1f", data.getX(), data.getY(), data.getZ()));
            ServerLevel spawnLevel = server.getLevel(data.getDimensionKey());
            sendKV(source, "Spawn Dim Status", spawnLevel != null ? "LOADED" : "NOT LOADED");
            if (spawnLevel != null) {
                sendKV(source, "Spawn Dim Players", spawnLevel.players().size());
                sendKV(source, "Spawn Dim Loaded Chunks", spawnLevel.getChunkSource().getLoadedChunksCount());
            }
        }

        sendKV(source, "Slot Bypass", SlotBypassConfig.VALUES.enabled.get() ? "ENABLED" : "DISABLED");
        sendKV(source, "Online Players", server.getPlayerList().getPlayerCount() + "/" + server.getMaxPlayers());
        sendKV(source, "First Join TP", SpawnConfig.COMMON.forceSpawnOnFirstJoin.get());
        sendKV(source, "Respawn TP", SpawnConfig.COMMON.forceSpawnOnRespawn.get());

        return 1;
    }

    private static int showSpawnInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        sendHeader(source, "Spawn Info");

        SpawnData data = SpawnData.get();
        sendKV(source, "Is Set", data.isSet());
        if (data.isSet()) {
            sendKV(source, "Dimension", data.getDimensionId());
            sendKV(source, "Position", String.format("%.2f, %.2f, %.2f", data.getX(), data.getY(), data.getZ()));
            sendKV(source, "Rotation", String.format("yaw=%.1f, pitch=%.1f", data.getYaw(), data.getPitch()));

            ServerLevel spawnLevel = source.getServer().getLevel(data.getDimensionKey());
            if (spawnLevel != null) {
                sendKV(source, "Day Time", spawnLevel.getDayTime());
                sendKV(source, "Weather", spawnLevel.isRaining() ? "RAIN" : (spawnLevel.isThundering() ? "THUNDER" : "CLEAR"));
                sendKV(source, "Players", spawnLevel.players().size());
            } else {
                send(source, "  Target dimension is NOT loaded!");
            }
        } else {
            send(source, "  Spawn is not configured. Use /arcadia_spawn setspawn.");
        }

        sendKV(source, "Time Locked", SpawnConfig.COMMON.timeLocked.get());
        return 1;
    }

    private static int showDimensionInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        sendHeader(source, "Dimension Config");
        sendKV(source, "Biome", SpawnConfig.COMMON.biome.get());
        sendKV(source, "Layers", SpawnConfig.COMMON.flatLayers.get().toString());
        sendKV(source, "Skylight", SpawnConfig.COMMON.hasSkylight.get());
        sendKV(source, "Ceiling", SpawnConfig.COMMON.hasCeiling.get());
        sendKV(source, "Ultrawarm", SpawnConfig.COMMON.ultrawarm.get());
        sendKV(source, "Natural", SpawnConfig.COMMON.natural.get());
        sendKV(source, "Coord Scale", SpawnConfig.COMMON.coordinateScale.get());
        sendKV(source, "Height", SpawnConfig.COMMON.minY.get() + " to " + (SpawnConfig.COMMON.minY.get() + SpawnConfig.COMMON.height.get()));
        sendKV(source, "Logical Height", SpawnConfig.COMMON.logicalHeight.get());
        sendKV(source, "Ambient Light", SpawnConfig.COMMON.ambientLight.get());
        sendKV(source, "Effects", SpawnConfig.COMMON.effects.get());
        sendKV(source, "Features", SpawnConfig.COMMON.latesAndFeatures.get());

        return 1;
    }

    private static int listLobbies(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<LobbyLocation> locations = LobbyManager.getLocations();

        sendHeader(source, "Lobby Locations (" + locations.size() + ")");

        if (locations.isEmpty()) {
            send(source, "  No lobby locations configured.");
            return 1;
        }

        for (int i = 0; i < locations.size(); i++) {
            LobbyLocation loc = locations.get(i);
            source.sendSuccess(() -> Component.literal("  [" + loc.name() + "] ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(String.format("%.1f, %.1f, %.1f @ %s",
                            loc.x(), loc.y(), loc.z(), loc.dimension().location()))
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" item=" + loc.item())
                            .withStyle(ChatFormatting.DARK_GRAY)), false);
            if (!loc.description().isEmpty()) {
                source.sendSuccess(() -> Component.literal("    desc: " + loc.description())
                        .withStyle(ChatFormatting.DARK_GRAY), false);
            }
        }

        return 1;
    }

    private static int showRtpData(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "Console cannot have RTP data.");
            return 0;
        }

        sendHeader(source, "RTP Data — " + player.getName().getString());
        RTPData data = player.getData(AttachmentRegistry.RTP_DATA);
        sendKV(source, "Usages", data.getUsages() + "/" + SpawnConfig.COMMON.rtpMaxUsage.get());
        BlockPos lastPos = data.getLastRtpPos();
        sendKV(source, "Last Position", lastPos.equals(BlockPos.ZERO) ? "NONE" :
                String.format("%d, %d, %d", lastPos.getX(), lastPos.getY(), lastPos.getZ()));
        sendKV(source, "RTP Radius", SpawnConfig.COMMON.rtpRadius.get());
        sendKV(source, "RTP Cooldown", SpawnConfig.COMMON.rtpCooldownSeconds.get() + "s");
        sendKV(source, "RTP Warmup", SpawnConfig.COMMON.rtpWarmupTicks.get() + " ticks");
        sendKV(source, "Max Attempts", SpawnConfig.COMMON.rtpMaxAttempts.get());

        return 1;
    }

    private static int resetRtpData(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        RTPData data = player.getData(AttachmentRegistry.RTP_DATA);
        data.reset();
        player.sendSystemMessage(ArcadiaMessages.success("RTP data reset!"));
        return 1;
    }

    private static int dumpConfig(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        sendHeader(source, "Active Configuration");

        // RTP
        sendKV(source, "[RTP] Radius", SpawnConfig.COMMON.rtpRadius.get());
        sendKV(source, "[RTP] Max Usage", SpawnConfig.COMMON.rtpMaxUsage.get());
        sendKV(source, "[RTP] Cooldown", SpawnConfig.COMMON.rtpCooldownSeconds.get() + "s");
        sendKV(source, "[RTP] Warmup", SpawnConfig.COMMON.rtpWarmupTicks.get() + " ticks");
        sendKV(source, "[RTP] Max Attempts", SpawnConfig.COMMON.rtpMaxAttempts.get());

        // Spawn TP
        sendKV(source, "[Spawn TP] Warmup", SpawnConfig.COMMON.spawnTpWarmupTicks.get() + " ticks");
        sendKV(source, "[Spawn TP] Cooldown", SpawnConfig.COMMON.spawnTpCooldownSeconds.get() + "s");

        // Lobby TP
        sendKV(source, "[Lobby TP] Warmup", SpawnConfig.COMMON.lobbyTpWarmupTicks.get() + " ticks");
        sendKV(source, "[Lobby TP] Cooldown", SpawnConfig.COMMON.lobbyTpCooldownSeconds.get() + "s");

        // Mob Spawning
        sendKV(source, "[Mobs] Monsters", SpawnConfig.COMMON.spawnMonsters.get());
        sendKV(source, "[Mobs] Creatures", SpawnConfig.COMMON.spawnCreatures.get());
        sendKV(source, "[Mobs] Ambient", SpawnConfig.COMMON.spawnAmbient.get());
        sendKV(source, "[Mobs] Water", SpawnConfig.COMMON.spawnWaterCreatures.get());
        sendKV(source, "[Mobs] Misc", SpawnConfig.COMMON.spawnMisc.get());

        // First Join
        sendKV(source, "[Join] Force First Join", SpawnConfig.COMMON.forceSpawnOnFirstJoin.get());
        sendKV(source, "[Join] Force Respawn", SpawnConfig.COMMON.forceSpawnOnRespawn.get());

        // Slot Bypass
        sendKV(source, "[Slots] Enabled", SlotBypassConfig.VALUES.enabled.get());
        sendKV(source, "[Slots] Max Slots", SlotBypassConfig.VALUES.maxSlots.get());
        sendKV(source, "[Slots] Fake Max", SlotBypassConfig.VALUES.fakeMaxSlotsEnabled.get());
        sendKV(source, "[Slots] Hide Join/Leave", SlotBypassConfig.VALUES.hideJoinLeaveMessages.get());

        return 1;
    }

    private static int showSlotInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        sendHeader(source, "Slot Bypass Info");
        sendKV(source, "Enabled", SlotBypassConfig.VALUES.enabled.get());
        sendKV(source, "Max Slots", SlotBypassConfig.VALUES.maxSlots.get());
        sendKV(source, "Online", server.getPlayerList().getPlayerCount());
        sendKV(source, "Real Max", server.getMaxPlayers());
        sendKV(source, "Fake Max Shown", SlotBypassConfig.VALUES.fakeMaxSlotsEnabled.get());
        sendKV(source, "Hide Join/Leave", SlotBypassConfig.VALUES.hideJoinLeaveMessages.get());
        sendKV(source, "Kick Message", SlotBypassConfig.VALUES.kickMessage.get());

        return 1;
    }

    private static int showPlayerInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            send(ctx.getSource(), "Console has no player data.");
            return 0;
        }

        CommandSourceStack source = ctx.getSource();
        sendHeader(source, "Player Info — " + player.getName().getString());
        sendKV(source, "UUID", player.getUUID().toString());
        sendKV(source, "Language", LocalizationManager.getPlayerLang(player));
        sendKV(source, "Dimension", player.level().dimension().location().toString());
        sendKV(source, "Position", String.format("%.2f, %.2f, %.2f", player.getX(), player.getY(), player.getZ()));
        sendKV(source, "Visited Tag", player.getTags().contains("arcadia_visited"));
        sendKV(source, "Op Level", player.getServer().getProfilePermissions(player.getGameProfile()));
        sendKV(source, "Game Mode", player.gameMode.getGameModeForPlayer().getName());

        return 1;
    }

    private static int forceReloadAll(CommandContext<CommandSourceStack> ctx) {
        LobbyManager.reload();
        LocalizationManager.init();
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(ArcadiaMessages.success("Full reload complete (lobbies + languages)."));
        } else {
            send(ctx.getSource(), "Full reload complete (lobbies + languages).");
        }
        return 1;
    }

    private static int resetVisitedTag(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        player.removeTag("arcadia_visited");
        player.sendSystemMessage(ArcadiaMessages.success("Visited tag removed. Next login will trigger first-join TP."));
        return 1;
    }

    private static int showTps(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        sendHeader(source, "Server Performance");

        long[] tickTimes = server.getTickTimesNanos();
        double avgMs = 0;
        double maxMs = 0;
        for (long t : tickTimes) {
            double ms = t / 1_000_000.0;
            avgMs += ms;
            if (ms > maxMs) maxMs = ms;
        }
        avgMs /= tickTimes.length;

        double tps = Math.min(20.0, 1000.0 / Math.max(avgMs, 1));

        sendKV(source, "TPS", String.format("%.1f", tps));
        sendKV(source, "Avg Tick", String.format("%.2f ms", avgMs));
        sendKV(source, "Max Tick", String.format("%.2f ms", maxMs));
        sendKV(source, "Tick Count", server.getTickCount());

        ServerLevel spawnLevel = server.getLevel(ModEvents.SPAWN_LEVEL_KEY);
        if (spawnLevel != null) {
            sendKV(source, "Spawn Dim Chunks", spawnLevel.getChunkSource().getLoadedChunksCount());
            sendKV(source, "Spawn Dim Entities", spawnLevel.getAllEntities().spliterator().getExactSizeIfKnown());
        }

        return 1;
    }

    private static int showLangInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();

        sendHeader(source, "Language Info");
        if (player != null) {
            sendKV(source, "Client Language", LocalizationManager.getPlayerLang(player));
            sendKV(source, "Test EN", LocalizationManager.getString(player, "arcadia_spawn.menu.title"));
        } else {
            send(source, "  Run as a player to see client language.");
        }

        return 1;
    }

    private static int testLangKey(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String key = StringArgumentType.getString(ctx, "key");

        if (player == null) {
            send(ctx.getSource(), "Must be a player.");
            return 0;
        }

        String result = LocalizationManager.getString(player, key);
        player.sendSystemMessage(Component.literal("Key: " + key).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Result: " + result).withStyle(ChatFormatting.WHITE));
        return 1;
    }
}
