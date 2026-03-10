package com.vyrriox.arcadiaspawn;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(ArcadiaSpawnMod.MOD_ID)
public class ArcadiaSpawnMod {
    public static final String MOD_ID = "arcadia_spawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcadiaSpawnMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register Config
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,
                com.vyrriox.arcadiaspawn.config.SpawnConfig.SPEC, "arcadia/arcadialobbyspawn/config.toml");

        // Register Slot Bypass Config
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                com.vyrriox.arcadiaspawn.config.SlotBypassConfig.SPEC, "arcadia/arcadialobbyspawn/slot_bypass.toml");

        // Register Menus - REMOVED for Server Only Mod
        // com.vyrriox.arcadiaspawn.lobby.ModMenuTypes.register(modEventBus);

        // Register Common Setup for Manager Init
        modEventBus.addListener(this::commonSetup);

        // Register Attachments
        com.vyrriox.arcadiaspawn.registry.AttachmentRegistry.register(modEventBus);
    }

    private void commonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.vyrriox.arcadiaspawn.lobby.LobbyManager.init();
            com.vyrriox.arcadiaspawn.lobby.LocalizationManager.init();
        });
    }
}
