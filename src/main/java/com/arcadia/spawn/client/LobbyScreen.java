package com.arcadia.spawn.client;

import com.arcadia.lib.client.ArcadiaTheme;
import com.arcadia.spawn.lobby.LobbyMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Steampunk-themed lobby screen using ArcadiaTheme.
 * Custom background for 3-row container (slots at y+18..y+71, player inv at y+84).
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
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Drop shadow
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x55000000);

        // Main background
        g.fill(x, y, x + w, y + h / 2, 0xF01E1A24);
        g.fill(x, y + h / 2, x + w, y + h, 0xF0141018);

        // Outer border
        ArcadiaTheme.drawBorder(g, x, y, w, h, ArcadiaTheme.BORDER_IDLE);

        // Top accent bar
        g.fill(x, y, x + w, y + 2, ArcadiaTheme.COPPER);

        // Corner rivets
        int rivet = 0xFF504030;
        g.fill(x + 1, y + 1, x + 3, y + 3, rivet);
        g.fill(x + w - 3, y + 1, x + w - 1, y + 3, rivet);
        g.fill(x + 1, y + h - 3, x + 3, y + h - 1, rivet);
        g.fill(x + w - 3, y + h - 3, x + w - 1, y + h - 1, rivet);

        // Header inner highlight
        g.fill(x + 1, y + 2, x + w - 1, y + 3, 0x18FFFFFF);

        int slotX = x + 7;

        // Content slot area (3 rows starting at y+17)
        ArcadiaTheme.drawSlotGrid(g, slotX, y + 17, 3);

        // Separator between content and player inventory
        int sepY = y + 17 + 3 * 18 + 3;
        g.fill(x + 7, sepY, x + w - 7, sepY + 1, ArcadiaTheme.withAlpha(ArcadiaTheme.COPPER, 0x33));

        // Player inventory slots (3 rows starting at y+84)
        ArcadiaTheme.drawSlotGrid(g, slotX, y + 83, 3);

        // Hotbar (1 row starting at y+142)
        ArcadiaTheme.drawSlotGrid(g, slotX, y + 141, 1);

        // Bottom accent bar
        g.fill(x, y + h - 2, x + w, y + h, ArcadiaTheme.darken(ArcadiaTheme.COPPER, 40));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.drawString(this.font, this.title, titleX + 1, 7, 0x22000000, false);
        graphics.drawString(this.font, this.title, titleX, 6, ArcadiaTheme.BRASS, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, ArcadiaTheme.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
