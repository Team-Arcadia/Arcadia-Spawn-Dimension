package com.arcadia.spawn.client;

import com.arcadia.lib.client.ArcadiaTheme;
import com.arcadia.spawn.lobby.LobbyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Steampunk-themed lobby screen using ArcadiaTheme.
 * Replaces the vanilla chest GUI for the /lobby command.
 *
 * @author vyrriox
 */
public class LobbyScreen extends AbstractContainerScreen<LobbyMenu> {

    public LobbyScreen(LobbyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 168;
        this.imageWidth = 176;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ArcadiaTheme.drawContainerBg(graphics, this.leftPos, this.topPos, this.imageWidth, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Centered title with copper theme
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.drawString(this.font, this.title, titleX + 1, 7, 0x22000000, false);
        graphics.drawString(this.font, this.title, titleX, 6, ArcadiaTheme.BRASS, false);
        // Player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, ArcadiaTheme.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
