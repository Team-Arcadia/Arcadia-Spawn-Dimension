package com.vyrriox.spawndimension.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.vyrriox.spawndimension.world.ModDimensions;
import com.vyrriox.spawndimension.world.SpawnData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;

import java.util.Set;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setlobbyspawn")
                .requires(source -> source.hasPermission(2))
                .executes(ModCommands::setSpawn));

        dispatcher.register(Commands.literal("spawn")
                .executes(ModCommands::teleportToSpawn));

        // Alias /lobby as requested in plan
        dispatcher.register(Commands.literal("lobby")
                .executes(ModCommands::teleportToSpawn));
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null)
            return 0;

        SpawnData data = SpawnData.get(context.getSource().getLevel());
        data.setSpawn(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

        context.getSource().sendSuccess(() -> Component.literal("Lobby spawn set to current location."), true);
        return 1;
    }

    private static int teleportToSpawn(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null)
            return 0;

        ServerLevel spawnLevel = context.getSource().getServer().getLevel(ModDimensions.SPAWN_LEVEL_KEY);
        if (spawnLevel == null) {
            context.getSource().sendFailure(Component.literal("Spawn dimension not found!"));
            return 0;
        }

        SpawnData data = SpawnData.get(spawnLevel);
        if (!data.isSet()) {
            context.getSource()
                    .sendFailure(Component.literal("Lobby spawn point has not been set yet. Use /setlobbyspawn"));
            return 0;
        }

        player.teleportTo(spawnLevel, data.getX(), data.getY(), data.getZ(), Set.of(), data.getYaw(), data.getPitch());
        return 1;
    }
}
