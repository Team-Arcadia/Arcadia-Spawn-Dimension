package com.arcadia.spawn.client;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.registry.SpawnModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side MOD-bus events for arcadia-spawn.
 *
 * @author vyrriox
 */
@EventBusSubscriber(modid = ArcadiaSpawnMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpawnClientEvents {

    private SpawnClientEvents() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SpawnModMenus.LOBBY_MENU.get(), LobbyScreen::new);
    }
}
