package com.vyrriox.spawndimension;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SpawnDimensionMod.MOD_ID)
public class SpawnDimensionMod {
    public static final String MOD_ID = "spawn_dimension";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpawnDimensionMod(IEventBus modEventBus) {
        // Register the CommonSetup method for modloading
        // modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class
        // (SpawnDimensionMod) to handle events.
        // NeoForge.EVENT_BUS.register(this);
        LOGGER.info("SpawnDimensionMod Initialized! If you see this, the class loaded.");
        LOGGER.info("SpawnDimensionMod: EVENT_BUS.register(this) is commented out.");
    }
}
