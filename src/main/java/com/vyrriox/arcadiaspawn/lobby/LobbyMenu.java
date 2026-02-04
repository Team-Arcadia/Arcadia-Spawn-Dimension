package com.vyrriox.arcadiaspawn.lobby;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import java.util.Set;

public class LobbyMenu extends AbstractContainerMenu {
    private final Container container;
    private final List<LobbyLocation> locations;

    public LobbyMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }

    public LobbyMenu(int containerId, Inventory playerInventory, Container container) {
        super(net.minecraft.world.inventory.MenuType.GENERIC_9x3, containerId);
        this.container = container;
        this.locations = LobbyManager.getLocations();

        checkContainerSize(container, 27);
        container.startOpen(playerInventory.player);

        // Always valid slots structure (0-26)
        // We define the slots regardless of content so client/server match
        for (int i = 0; i < 27; i++) {
            this.addSlot(new Slot(container, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Only populate items on Server Side to avoid desync/ghost items
        if (!playerInventory.player.level().isClientSide()) {
            int slotIndex = 0;
            for (LobbyLocation loc : locations) {
                if (slotIndex >= 27)
                    break;

                // Resolve item from string or default to paper
                net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation.parse(loc.item()));
                if (item == Items.AIR) {
                    item = Items.PAPER;
                }
                ItemStack icon = new ItemStack(item);

                // Use DataComponents for 1.21 standard
                icon.set(DataComponents.CUSTOM_NAME,
                        Component.literal(loc.name()).withStyle(net.minecraft.ChatFormatting.GREEN)
                                .withStyle(net.minecraft.ChatFormatting.BOLD));

                if (loc.description() != null && !loc.description().isEmpty()) {
                    icon.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
                            java.util.List.of(Component.literal(loc.description())
                                    .withStyle(net.minecraft.ChatFormatting.GRAY))));
                }

                container.setItem(slotIndex, icon);
                slotIndex++;
            }
        }

        // Player Inventory (Standard position)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // No shifting
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Prevent any interaction with the menu slots other than clicking to teleport
        if (slotId >= 0 && slotId < 27) {
            // Logic runs on server mainly
            if (slotId < locations.size()) {
                LobbyLocation loc = locations.get(slotId);
                teleportPlayer(player, loc);
            }
            return; // Cancel default action
        }

        // Allow player inventory interaction
        super.clicked(slotId, button, clickType, player);
    }

    private void teleportPlayer(Player player, LobbyLocation loc) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel targetLevel = serverPlayer.server.getLevel(loc.dimension());
            if (targetLevel != null) {
                serverPlayer.closeContainer();
                serverPlayer.teleportTo(targetLevel, loc.x(), loc.y(), loc.z(), Set.of(), loc.yaw(), loc.pitch());
                serverPlayer.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                serverPlayer.displayClientMessage(
                        LocalizationManager.getComponent(serverPlayer, "arcadiaspawn.teleport.success", loc.name())
                                .withStyle(net.minecraft.ChatFormatting.GREEN),
                        true);
            } else {
                serverPlayer.displayClientMessage(
                        LocalizationManager.getComponent(serverPlayer, "arcadiaspawn.teleport.fail"), true);
            }
        }
    }
}
