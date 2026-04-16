package com.arcadia.spawn.registry;

import com.arcadia.spawn.ArcadiaSpawnMod;
import com.arcadia.spawn.lobby.LobbyMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SpawnModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ArcadiaSpawnMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LobbyMenu>> LOBBY_MENU =
            MENUS.register("lobby_menu", () -> new MenuType<>(LobbyMenu::new, FeatureFlags.VANILLA_SET));

    private SpawnModMenus() {}
}
