package com.arcadia.spawn;

import com.arcadia.lib.ArcadiaModRegistry;
import com.arcadia.lib.client.ArcadiaModCard;
import com.arcadia.spawn.config.SlotBypassConfig;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.lobby.LobbyManager;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.network.C2SOpenLobby;
import com.arcadia.spawn.network.SpawnNetworking;
import com.arcadia.spawn.registry.AttachmentRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(ArcadiaSpawnMod.MOD_ID)
public class ArcadiaSpawnMod {
    public static final String MOD_ID = "arcadia_spawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcadiaSpawnMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON,
                SpawnConfig.SPEC, "arcadia/spawn/config.toml");

        modContainer.registerConfig(ModConfig.Type.SERVER,
                SlotBypassConfig.SPEC, "arcadia/spawn/slot_bypass.toml");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(SpawnNetworking::onRegisterPayloads);

        AttachmentRegistry.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LobbyManager.init();
            LocalizationManager.init();

            // Register hub card — sortOrder 1 = first position in Hub
            ArcadiaModRegistry.registerCard(new ArcadiaModCard(
                    "spawn",
                    "\uD83C\uDFE0",
                    "arcadia_spawn.hub.title",
                    "arcadia_spawn.hub.subtitle",
                    0x55AA55,
                    1,
                    true
            ));

            // Card click handler: send our own C2S packet to open lobby menu
            // This bypasses prestige's dashboard entirely
            ArcadiaModRegistry.registerCardClickHandler("spawn", () -> {
                PacketDistributor.sendToServer(new C2SOpenLobby());
            });

            LOGGER.info("Arcadia Spawn initialized — hub card registered at position 1.");
        });
    }
}
