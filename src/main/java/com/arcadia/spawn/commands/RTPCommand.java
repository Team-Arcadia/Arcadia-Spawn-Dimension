package com.arcadia.spawn.commands;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.spawn.commands.TeleportHelper;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.data.RTPData;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.registry.AttachmentRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class RTPCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arcadiartp")
                .executes(RTPCommand::executeRtp));
    }

    private static int executeRtp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        // Destination is always the Overworld (RTP explores the Overworld radius),
        // but the command itself is callable from ANY dimension — including
        // arcadia:spawn where players land on first join and expect to hop out
        // via RTP. The previous overworld-only gate broke that flow.
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return 0;

        RTPData data = player.getData(AttachmentRegistry.RTP_DATA);
        int maxUsage = SpawnConfig.COMMON.rtpMaxUsage.get();
        int currentUsage = data.getUsages();

        // Limit reached — teleport to last position
        if (currentUsage >= maxUsage) {
            BlockPos lastPos = data.getLastRtpPos();
            if (lastPos != null && !lastPos.equals(BlockPos.ZERO)) {
                doTeleport(player, overworld, lastPos);
                player.sendSystemMessage(ArcadiaMessages.warning(
                        LocalizationManager.getString(player, "arcadia_spawn.command.rtp.limit_reached")));
            } else {
                player.sendSystemMessage(ArcadiaMessages.error(
                        LocalizationManager.getString(player, "arcadia_spawn.command.rtp.no_last_pos")));
            }
            return 1;
        }

        // Find safe position
        int maxAttempts = SpawnConfig.COMMON.rtpMaxAttempts.get();
        BlockPos newPos = findRandomSafePos(overworld, maxAttempts);
        if (newPos != null) {
            data.incrementUsages();
            data.setLastRtpPos(newPos);
            doTeleport(player, overworld, newPos);
            player.sendSystemMessage(ArcadiaMessages.success(
                    LocalizationManager.getString(player, "arcadia_spawn.command.rtp.success",
                            newPos.getX(), newPos.getY(), newPos.getZ())));
            return 1;
        } else {
            player.sendSystemMessage(ArcadiaMessages.error(
                    LocalizationManager.getString(player, "arcadia_spawn.command.rtp.fail_safe")));
            return 0;
        }
    }

    private static void doTeleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        int warmup = SpawnConfig.COMMON.rtpWarmupTicks.get();
        int cooldownMs = SpawnConfig.COMMON.rtpCooldownSeconds.get() * 1000;
        Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        TeleportHelper.teleportWithWarmup(player, target, level, warmup, cooldownMs, "rtp");
    }

    /**
     * Maximum number of chunks this command may force-generate on the server thread
     * per invocation. Random candidates in unexplored terrain require a blocking
     * chunk load+generate; without a cap a single /arcadiartp could generate dozens
     * of distant chunks on the main thread and freeze the server for seconds.
     * Already-loaded chunks are always evaluated (zero cost) regardless of this cap.
     */
    private static final int MAX_FORCED_CHUNK_LOADS = 8;

    /** Returns a safe position, or {@code null} when no safe spot was found (caller reports failure). */
    private static BlockPos findRandomSafePos(ServerLevel level, int maxAttempts) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int radius = SpawnConfig.COMMON.rtpRadius.get();
        int minBuild = level.getMinBuildHeight() + 5;
        int maxBuild = level.getMaxBuildHeight() - 5;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

        int forcedLoads = 0;
        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            int x = rand.nextInt(-radius, radius + 1);
            int z = rand.nextInt(-radius, radius + 1);
            int cx = x >> 4, cz = z >> 4;

            // Prefer already-loaded chunks (no stall). Only force-generate up to
            // MAX_FORCED_CHUNK_LOADS chunks so the server thread can't freeze.
            if (level.getChunkSource().getChunkNow(cx, cz) == null) {
                if (forcedLoads >= MAX_FORCED_CHUNK_LOADS) continue;
                forcedLoads++;
                level.getChunk(cx, cz); // bounded blocking load+generate
            }

            // The chunk is now resident, so getHeight/getFluidState/getBlockState
            // below won't trigger any additional blocking load.
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (y < minBuild || y > maxBuild) continue;

            cursor.set(x, y, z);
            below.set(x, y - 1, z);

            if (!level.getFluidState(cursor).isEmpty() || !level.getFluidState(below).isEmpty()) continue;
            if (level.getBlockState(below).isAir()) continue;

            return cursor.immutable();
        }

        // No safe position found — signal failure so the caller shows the localized
        // "fail_safe" message instead of silently teleporting the player to (0,0,0).
        return null;
    }
}
