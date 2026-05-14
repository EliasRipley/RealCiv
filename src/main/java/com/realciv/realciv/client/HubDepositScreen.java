package com.realciv.realciv.client;

import com.daqem.uilib.client.gui.AbstractContainerScreen;
import com.daqem.uilib.client.gui.component.SolidColorComponent;
import com.daqem.uilib.client.gui.component.AbstractComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.realciv.realciv.hub.CommunityHubDepositMenu;

public class HubDepositScreen extends AbstractContainerScreen<CommunityHubDepositMenu> {
    private static final int BG_WIDTH = 420;
    private static final int BG_HEIGHT = 256;

    private static final int SLOTS_LEFT = 8;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 9;
    private static final int DEPOSIT_ROWS = 6;
    private static final int INV_Y = 140;
    private static final int HOTBAR_Y = 198;

    private static final int LABEL_X = 190;

    public HubDepositScreen(CommunityHubDepositMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(2, (width - BG_WIDTH) / 2);
        topPos = Math.max(2, (height - BG_HEIGHT) / 2);
    }

    @Override
    public void startScreen() {
        setBackground(null);

        addComponent(new SolidColorComponent(0, 0, width, height, 0xC0000000));
        addComponent(new SolidColorComponent(leftPos, topPos, BG_WIDTH, BG_HEIGHT, 0xFF14171B));
        addComponent(new SolidColorComponent(leftPos + 3, topPos + 3, BG_WIDTH - 6, BG_HEIGHT - 6, 0xFF252D36));
        addComponent(new SolidColorComponent(leftPos + 8, topPos + 40, BG_WIDTH - 16, BG_HEIGHT - 48, 0xFF1A2129));
        addComponent(new SolidColorComponent(leftPos + 8, topPos + 40, BG_WIDTH - 16, 2, 0xFFFF8A3D));

        drawSlotGrid(leftPos + SLOTS_LEFT, topPos + 18, COLS, DEPOSIT_ROWS);
        drawSlotGrid(leftPos + SLOTS_LEFT, topPos + INV_Y, COLS, 3);
        drawSlotGrid(leftPos + SLOTS_LEFT, topPos + HOTBAR_Y, COLS, 1);

        // Right-side labels
        var font = Minecraft.getInstance().font;
        addComponent(new TitleLabel(leftPos + LABEL_X, topPos + 18, "Deposit Area", 0xFFF2F6FB));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + 32,
                "Place items here to", 0xFF9DB0C2));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + 44,
                "contribute to the", 0xFF9DB0C2));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + 56,
                "community hub.", 0xFF9DB0C2));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + 72,
                "Close the screen to", 0xFF78909C));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + 84,
                "finalize deposit.", 0xFF78909C));

        addComponent(new TitleLabel(leftPos + LABEL_X, topPos + INV_Y - 6, "Your Inventory", 0xFF9DB0C2));

        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + HOTBAR_Y + 4,
                "Drag items from your", 0xFF78909C));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + HOTBAR_Y + 16,
                "inventory into the", 0xFF78909C));
        addComponent(new BodyLabel(leftPos + LABEL_X, topPos + HOTBAR_Y + 28,
                "deposit area above.", 0xFF78909C));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float delta, int mouseX, int mouseY) {
    }

    private void drawSlotGrid(int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = x + col * SLOT_SIZE;
                int sy = y + row * SLOT_SIZE;
                addComponent(new SolidColorComponent(sx, sy, SLOT_SIZE, SLOT_SIZE, 0xFF555555));
                addComponent(new SolidColorComponent(sx + 1, sy + 1, 16, 16, 0xFF2A2E3A));
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.literal("Community Hub Deposit"),
                leftPos + 14, topPos + 8, 0xFFF2F6FB, false);
        graphics.drawString(font, Component.literal("Items are returned on close if not accepted"),
                leftPos + 14, topPos + 22, 0xFF9DB0C2, false);
    }

    @Override
    public void onTickScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class TitleLabel extends AbstractComponent<TitleLabel> {
        private final String text;
        private final int color;

        TitleLabel(int x, int y, String text, int color) {
            super(null, x, y, BG_WIDTH - 190, Minecraft.getInstance().font.lineHeight);
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.drawString(Minecraft.getInstance().font, Component.literal(text), 0, 0, color, false);
        }
    }

    private static class BodyLabel extends AbstractComponent<BodyLabel> {
        private final String text;
        private final int color;

        BodyLabel(int x, int y, String text, int color) {
            super(null, x, y, BG_WIDTH - 190, Minecraft.getInstance().font.lineHeight);
            this.text = text;
            this.color = color;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            graphics.drawString(Minecraft.getInstance().font, Component.literal(text), 0, 0, color, false);
        }
    }
}
