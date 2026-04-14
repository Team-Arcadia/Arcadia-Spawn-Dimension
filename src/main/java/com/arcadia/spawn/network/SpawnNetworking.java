package com.arcadia.spawn.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SpawnNetworking {

    private SpawnNetworking() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                C2SOpenLobby.TYPE,
                C2SOpenLobby.STREAM_CODEC,
                C2SOpenLobby::handle
        );
    }
}
