package com.arcadia.spawn.lobby;

import com.arcadia.lib.ArcadiaMessages;
import com.arcadia.spawn.commands.TeleportHelper;
import com.arcadia.spawn.config.SpawnConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import com.arcadia.spawn.registry.SpawnModMenus;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LobbyMenu extends AbstractContainerMenu {
    private final Container container;
    private final List<LobbyLocation> locations;

    public LobbyMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }

    public LobbyMenu(int containerId, Inventory playerInventory, Container container) {
        super(SpawnModMenus.LOBBY_MENU.get(), containerId);
        this.container = container;
        this.locations = LobbyManager.getLocations();

        checkContainerSize(container, 27);
        container.startOpen(playerInventory.player);

        for (int i = 0; i < 27; i++) {
            this.addSlot(new Slot(container, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // Server-side only: populate items
        if (!playerInventory.player.level().isClientSide()) {
            // Fill border with glass panes
            for (int i = 0; i < 27; i++) {
                ItemStack pane = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
                pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                container.setItem(i, pane);
            }

            // Place lobby items in center area
            int slotIndex = 10; // Start at row 2, col 2
            for (LobbyLocation loc : locations) {
                if (slotIndex > 16) break; // Max 7 items in center row

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loc.item()));
                if (item == Items.AIR) item = Items.PAPER;
                ItemStack icon = new ItemStack(item);

                icon.set(DataComponents.CUSTOM_NAME,
                        Component.literal(loc.name())
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                if (!loc.description().isEmpty()) {
                    icon.set(DataComponents.LORE, new ItemLore(
                            List.of(Component.literal(loc.description()).withStyle(ChatFormatting.GRAY),
                                    Component.literal("").withStyle(ChatFormatting.DARK_GRAY),
                                    Component.translatable("arcadia_spawn.lobby.click_tp")
                                            .withStyle(ChatFormatting.YELLOW))));
                } else {
                    icon.set(DataComponents.LORE, new ItemLore(
                            List.of(Component.translatable("arcadia_spawn.lobby.click_tp")
                                    .withStyle(ChatFormatting.YELLOW))));
                }

                container.setItem(slotIndex, icon);
                slotIndex++;
            }
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 27) {
            if (player instanceof ServerPlayer serverPlayer) {
                // Find which lobby location was clicked
                int lobbyIndex = slotId - 10;
                if (lobbyIndex >= 0 && lobbyIndex < locations.size()) {
                    teleportPlayer(serverPlayer, locations.get(lobbyIndex));
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void teleportPlayer(ServerPlayer player, LobbyLocation loc) {
        ServerLevel targetLevel = player.server.getLevel(loc.dimension());
        if (targetLevel == null) {
            player.displayClientMessage(
                    LocalizationManager.getComponent(player, "arcadia_spawn.teleport.fail"), true);
            return;
        }

        player.closeContainer();

        int warmup = SpawnConfig.COMMON.lobbyTpWarmupTicks.get();
        int cooldownMs = SpawnConfig.COMMON.lobbyTpCooldownSeconds.get() * 1000;

        TeleportHelper.teleportWithWarmup(player,
                new Vec3(loc.x(), loc.y(), loc.z()),
                targetLevel, warmup, cooldownMs, "lobby_tp");

        player.sendSystemMessage(ArcadiaMessages.success(
                LocalizationManager.getString(player, "arcadia_spawn.teleport.success", loc.name())));
    }
}
