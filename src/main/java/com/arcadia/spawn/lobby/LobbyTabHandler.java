package com.arcadia.spawn.lobby;

import com.arcadia.lib.dashboard.DashboardTabHandler;
import com.arcadia.spawn.commands.TeleportHelper;
import com.arcadia.spawn.config.SpawnConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * DashboardTabHandler for the Lobby tab inside the Arcadia Dashboard.
 * Populates slots 9-53 with lobby warp points.
 */
public class LobbyTabHandler implements DashboardTabHandler {

    private List<LobbyLocation> locations;

    @Override
    public void buildTab(SimpleContainer container, ServerPlayer player) {
        locations = LobbyManager.getLocations();

        // Fill with glass panes
        for (int i = 9; i < 54; i++) {
            ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
            pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            container.setItem(i, pane);
        }

        // Place lobby items in the center rows
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < Math.min(locations.size(), slots.length); i++) {
            LobbyLocation loc = locations.get(i);

            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loc.item()));
            if (item == Items.AIR) item = Items.PAPER;
            ItemStack icon = new ItemStack(item);

            icon.set(DataComponents.CUSTOM_NAME,
                    Component.literal(loc.name())
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            if (!loc.description().isEmpty()) {
                icon.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal(loc.description()).withStyle(ChatFormatting.GRAY),
                        Component.literal(""),
                        Component.literal("Click to teleport | Cliquez pour vous tp")
                                .withStyle(ChatFormatting.YELLOW))));
            } else {
                icon.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("Click to teleport | Cliquez pour vous tp")
                                .withStyle(ChatFormatting.YELLOW))));
            }

            container.setItem(slots[i], icon);
        }
    }

    @Override
    public boolean handleClick(int slotId, int button, ServerPlayer player, Runnable refreshTab) {
        if (locations == null) return false;

        // Map slot ID back to lobby index
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < Math.min(locations.size(), slots.length); i++) {
            if (slots[i] == slotId) {
                LobbyLocation loc = locations.get(i);
                ServerLevel targetLevel = player.server.getLevel(loc.dimension());
                if (targetLevel != null) {
                    player.closeContainer();

                    int warmup = SpawnConfig.COMMON.lobbyTpWarmupTicks.get();
                    int cooldownMs = SpawnConfig.COMMON.lobbyTpCooldownSeconds.get() * 1000;

                    TeleportHelper.teleportWithWarmup(player,
                            new Vec3(loc.x(), loc.y(), loc.z()),
                            targetLevel, warmup, cooldownMs, "lobby_tp");

                    player.sendSystemMessage(
                            com.arcadia.lib.ArcadiaMessages.success(
                                    LocalizationManager.getString(player, "arcadia_spawn.teleport.success", loc.name())));
                } else {
                    player.sendSystemMessage(
                            com.arcadia.lib.ArcadiaMessages.error(
                                    LocalizationManager.getString(player, "arcadia_spawn.teleport.fail")));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack getNavBarItem(ServerPlayer player) {
        ItemStack icon = new ItemStack(Items.COMPASS);
        icon.set(DataComponents.CUSTOM_NAME,
                Component.literal("Lobby")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        icon.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(LobbyManager.getLocationCount() + " warps")
                        .withStyle(ChatFormatting.GRAY))));
        return icon;
    }
}
