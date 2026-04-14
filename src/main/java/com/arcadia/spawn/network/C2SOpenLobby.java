package com.arcadia.spawn.network;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.commands.SpawnCommands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server packet to request lobby menu opening.
 * Sent when the player clicks the Spawn card in the Arcadia Hub.
 */
public record C2SOpenLobby() implements CustomPacketPayload {

    public static final Type<C2SOpenLobby> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcadiaSpawnMod.MOD_ID, "open_lobby"));

    public static final StreamCodec<FriendlyByteBuf, C2SOpenLobby> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {},
                    buf -> new C2SOpenLobby()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SOpenLobby pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                SpawnCommands.openLobbyForPlayer(player);
            }
        });
    }
}
