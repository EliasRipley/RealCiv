package com.realciv.realciv.client;

import com.realciv.realciv.hub.RationDraftMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class RationDraftScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<RationDraftMenu> {
    private static final int BG_W = 420;
    private static final int BG_H = 276;
    private static final int SLOT_SIZE = 18;
    private static final int DRAFT_Y = 54;
    private static final int INV_Y = 168;
    private static final int HOTBAR_Y = 226;

    public RationDraftScreen(RationDraftMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = BG_W;
        imageHeight = BG_H;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(2, (width - BG_W) / 2);
        topPos = Math.max(2, (height - BG_H) / 2);

        addRenderableWidget(Button.builder(Component.literal("Apply + Close"), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RationDraftMenu.ACTION_APPLY_AND_CLOSE);
                    }
                })
                .bounds(leftPos + BG_W - 198, topPos + 8, 94, 14)
                .build());
        addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
                .bounds(leftPos + BG_W - 26, topPos + 8, 18, 14)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB0111318);
        graphics.fill(leftPos, topPos, leftPos + BG_W, topPos + BG_H, 0xFF0F141C);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + BG_W - 3, topPos + BG_H - 3, 0xFF1E2734);
        graphics.fill(leftPos + 8, topPos + 50, leftPos + BG_W - 8, topPos + BG_H - 8, 0xFF141B25);
        graphics.fill(leftPos + 8, topPos + 50, leftPos + BG_W - 8, topPos + 52, 0xFF4DB6AC);
        graphics.fill(leftPos + 190, topPos + 54, leftPos + BG_W - 14, topPos + BG_H - 14, 0x181B2534);
        graphics.fill(leftPos + 190, topPos + 166, leftPos + BG_W - 14, topPos + 168, 0xFF2B3545);

        drawSlotGrid(graphics, leftPos + 8, topPos + DRAFT_Y, 9, 6);
        drawSlotGrid(graphics, leftPos + 8, topPos + INV_Y, 9, 3);
        drawSlotGrid(graphics, leftPos + 8, topPos + HOTBAR_Y, 9, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.literal("Ration Draft Editor"), 14, 8, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("Drag items and stack sizes to define daily ration allowances"), 14, 22, 0xFF9DB0C2, false);
        String civLabel = menu.civilizationId().isBlank() ? "-" : menu.civilizationId();
        graphics.drawString(font, Component.literal("Civilization: " + civLabel), 14, 32, 0xFF90CAF9, false);

        graphics.drawString(font, Component.literal("How It Works"), 190, 58, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("1. Move items into the draft grid."), 190, 72, 0xFF9DB0C2, false);
        graphics.drawString(font, Component.literal("2. Stack size = allowance per day."), 190, 84, 0xFF9DB0C2, false);
        graphics.drawString(font, Component.literal("3. Click Apply + Close to save."), 190, 96, 0xFF9DB0C2, false);
        graphics.drawString(font, Component.literal("Items are always returned to you."), 190, 108, 0xFF80CBC4, false);
        graphics.drawString(font, Component.literal("Apply replaces the full allowance set."), 190, 120, 0xFF78909C, false);

        graphics.drawString(font, Component.literal("Current Allowances"), 190, 136, 0xFFF2F6FB, false);
        int y = 148;
        if (menu.allowancePreview().isEmpty()) {
            graphics.drawString(font, Component.literal("- none configured"), 190, y, 0xFF78909C, false);
            return;
        }
        int shown = 0;
        for (RationDraftMenu.AllowancePreviewEntry entry : menu.allowancePreview()) {
            if (shown >= 8) {
                break;
            }
            String itemName = resolveItemDisplayName(entry.itemId());
            String line = "- " + itemName + " x" + Math.max(0, entry.dailyAllowance()) + "/day";
            graphics.drawString(font, Component.literal(font.plainSubstrByWidth(line, 210)), 190, y, 0xFF9DB0C2, false);
            y += 10;
            shown++;
        }
        int hidden = Math.max(0, menu.allowancePreview().size() - shown);
        if (hidden > 0) {
            graphics.drawString(font, Component.literal("+" + hidden + " more"), 190, y, 0xFF78909C, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void drawSlotGrid(GuiGraphics graphics, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = x + col * SLOT_SIZE;
                int sy = y + row * SLOT_SIZE;
                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF555555);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xFF2A2E3A);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String resolveItemDisplayName(String itemIdRaw) {
        try {
            ResourceLocation itemId = ResourceLocation.parse(itemIdRaw);
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            if (item == Items.AIR) {
                return itemIdRaw;
            }
            return item.getDescription().getString();
        } catch (Exception ignored) {
            return itemIdRaw;
        }
    }
}
