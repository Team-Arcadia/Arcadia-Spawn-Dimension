package com.arcadia.spawn;

import com.arcadia.lib.ArcadiaModRegistry;
import com.arcadia.lib.client.ArcadiaModCard;
import com.arcadia.spawn.config.SlotBypassConfig;
import com.arcadia.spawn.config.SpawnConfig;
import com.arcadia.spawn.lobby.LobbyManager;
import com.arcadia.spawn.lobby.LocalizationManager;
import com.arcadia.spawn.registry.AttachmentRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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

            // Register card click handler to open lobby menu via server action
            ArcadiaModRegistry.registerCardClickHandler("spawn", () -> {
                ArcadiaModRegistry.executeClientAction("arcadia_spawn:open_lobby");
            });

            // Register server action for lobby open
            ArcadiaModRegistry.registerServerAction("arcadia_spawn:open_lobby", player -> {
                com.arcadia.spawn.commands.SpawnCommands.openLobbyForPlayer(player);
            });

            LOGGER.info("Arcadia Spawn initialized — hub card registered at position 1.");
        });
    }
}
