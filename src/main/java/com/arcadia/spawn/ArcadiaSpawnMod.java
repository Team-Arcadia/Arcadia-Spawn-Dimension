package com.arcadia.spawn;

import com.arcadia.lib.ArcadiaModRegistry;
import com.arcadia.lib.client.ArcadiaModCard;
import com.arcadia.lib.data.DatabaseManager;
import com.arcadia.spawn.config.SlotBypassConfig;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.lobby.LobbyManager;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.network.C2SOpenLobby;
import com.arcadia.spawn.network.SpawnNetworking;
import com.arcadia.spawn.registry.AttachmentRegistry;
import com.arcadia.spawn.registry.SpawnModMenus;
import com.arcadia.spawn.tablist.CrossServerDb;
import com.arcadia.spawn.tablist.TabListConfig;
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

        modContainer.registerConfig(ModConfig.Type.SERVER,
                TabListConfig.SPEC, "arcadia/spawn/tablist.toml");

        // Register our cross-server table with arcadia-lib's DatabaseManager.
        // Safe to call before DB init — DatabaseManager queues table registrations.
        try {
            DatabaseManager.registerTables(CrossServerDb.INSTANCE);
        } catch (Throwable t) {
            LOGGER.warn("Could not register tablist table with arcadia-lib: {}", t.getMessage());
        }

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(SpawnNetworking::onRegisterPayloads);

        AttachmentRegistry.register(modEventBus);
        SpawnModMenus.MENUS.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LobbyManager.init();
            LocalizationManager.init();

            // Register hub card — row 0, sortOrder 0, tabIndex -1 (custom click handler)
            ArcadiaModRegistry.registerCard(new ArcadiaModCard(
                    "spawn",
                    "\uD83C\uDFE0",
                    "arcadia_spawn.hub.title",
                    "arcadia_spawn.hub.subtitle",
                    0x55AA55,
                    0,      // sortOrder = first in row
                    0,      // row = top row
                    -1,     // tabIndex = -1 (uses cardClickHandler, not dashboard tab)
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
