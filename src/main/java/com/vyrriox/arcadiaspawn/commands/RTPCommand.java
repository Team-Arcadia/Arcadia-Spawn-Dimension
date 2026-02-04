package com.vyrriox.arcadiaspawn.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.vyrriox.arcadiaspawn.config.SpawnConfig;
import com.vyrriox.arcadiaspawn.data.RTPData;
import com.vyrriox.arcadiaspawn.lobby.LocalizationManager;
import com.vyrriox.arcadiaspawn.registry.AttachmentRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;
import java.util.Set;

public class RTPCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arcadiartp")
                .executes(RTPCommand::executeRtp));
    }

    private static int executeRtp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null)
            return 0;

        // Force Overworld
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null)
            return 0;

        // Get persistent data
        RTPData data = player.getData(AttachmentRegistry.RTP_DATA);
        int maxUsage = SpawnConfig.COMMON.rtpMaxUsage.get();
        int currentUsage = data.getUsages();

        if (currentUsage >= maxUsage) {
            // Teleport to last RTP spot if limit reached
            BlockPos lastPos = data.getLastRtpPos();
            if (lastPos != null && !lastPos.equals(BlockPos.ZERO)) {
                player.teleportTo(overworld, lastPos.getX() + 0.5, lastPos.getY(), lastPos.getZ() + 0.5, Set.of(),
                        player.getYRot(), player.getXRot());
                context.getSource().sendSuccess(
                        () -> LocalizationManager.getComponent(player, "arcadiaspawn.command.rtp.limit_reached"), true);
            } else {
                context.getSource()
                        .sendFailure(LocalizationManager.getComponent(player, "arcadiaspawn.command.rtp.no_last_pos"));
            }
            return 1;
        }

        // Find new random position in Overworld
        BlockPos newPos = findRandomSafePos(overworld);
        if (newPos != null) {
            player.teleportTo(overworld, newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5, Set.of(),
                    player.getYRot(), player.getXRot());

            // Update data
            data.incrementUsages();
            data.setLastRtpPos(newPos);

            context.getSource().sendSuccess(() -> LocalizationManager.getComponent(player,
                    "arcadiaspawn.command.rtp.success", newPos.getX(), newPos.getY(), newPos.getZ()), true);
            return 1;
        } else {
            context.getSource()
                    .sendFailure(LocalizationManager.getComponent(player, "arcadiaspawn.command.rtp.fail_safe"));
            return 0;
        }
    }

    private static BlockPos findRandomSafePos(ServerLevel level) {
        Random rand = new Random();
        int radius = SpawnConfig.COMMON.rtpRadius.get();

        for (int attempts = 0; attempts < 50; attempts++) {
            // Random position within radius
            int x = rand.nextInt(radius * 2 + 1) - radius;
            int z = rand.nextInt(radius * 2 + 1) - radius;

            // Force load chunk
            level.getChunk(x >> 4, z >> 4);

            // Get surface Y
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

            // Safety bounds
            if (y < level.getMinBuildHeight() + 5 || y > level.getMaxBuildHeight() - 5) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();

            try {
                // Skip if in liquid
                if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(below).isEmpty()) {
                    continue;
                }

                // Check there's ground
                if (level.getBlockState(below).isAir()) {
                    continue;
                }

                return pos;

            } catch (Exception e) {
                continue;
            }
        }

        // Fallback: spawn point
        return new BlockPos(0, level.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0), 0);
    }
}
