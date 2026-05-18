package com.realciv.realciv.client;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.KeyModifiers;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ResponsiveRealCivScreenWrapper extends ScreenWrapper {
    private final RealCivScreen wrappedGui;
    private final TooltipList tooltipList = new TooltipList();
    private float uiScale;
    private int viewportX;
    private int viewportY;
    private int viewportW;
    private int viewportH;

    public ResponsiveRealCivScreenWrapper(RealCivScreen gui) {
        super(gui);
        this.wrappedGui = gui;
        this.wrappedGui.setResponsiveWrapperMode(true);
        this.uiScale = 1F;
    }

    @Override
    public void init() {
        super.init();
        wrappedGui.initGui();
        recomputeViewport();
    }

    private void recomputeViewport() {
        int screenW = minecraft != null ? minecraft.getWindow().getGuiScaledWidth() : width;
        int screenH = minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : height;
        int designW = wrappedGui.designWidth();
        int designH = wrappedGui.designHeight();

        float widthScale = screenW / (float) designW;
        float heightScale = screenH / (float) designH;
        uiScale = Math.min(1F, Math.min(widthScale, heightScale));
        if (!(uiScale > 0F)) {
            uiScale = 1F;
        }

        viewportW = Math.max(1, Math.round(designW * uiScale));
        viewportH = Math.max(1, Math.round(designH * uiScale));
        viewportX = (screenW - viewportW) / 2;
        viewportY = (screenH - viewportH) / 2;
    }

    private double toDesignX(double x) {
        return (x - viewportX) / uiScale;
    }

    private double toDesignY(double y) {
        return (y - viewportY) / uiScale;
    }

    private int toDesignMouseX(double x) {
        return (int) Math.floor(toDesignX(x));
    }

    private int toDesignMouseY(double y) {
        return (int) Math.floor(toDesignY(y));
    }

    private double toDesignDelta(double value) {
        return value / uiScale;
    }

    @Override
    public boolean isPauseScreen() {
        return wrappedGui.doesGuiPauseGame();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        int mx = toDesignMouseX(x);
        int my = toDesignMouseY(y);
        wrappedGui.updateMouseOver(mx, my);

        if (button == MouseButton.BACK.id) {
            wrappedGui.onBack();
            return true;
        } else {
            return wrappedGui.mousePressed(MouseButton.get(button)) || super.mouseClicked(x, y, button);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        int mx = toDesignMouseX(x);
        int my = toDesignMouseY(y);
        wrappedGui.updateMouseOver(mx, my);
        wrappedGui.mouseReleased(MouseButton.get(button));
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dirX, double dirY) {
        int mx = toDesignMouseX(x);
        int my = toDesignMouseY(y);
        wrappedGui.updateMouseOver(mx, my);
        return wrappedGui.mouseScrolled(dirY) || super.mouseScrolled(x, y, dirX, dirY);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        int mx = toDesignMouseX(x);
        int my = toDesignMouseY(y);
        wrappedGui.updateMouseOver(mx, my);
        return wrappedGui.mouseDragged(button, toDesignDelta(dragX), toDesignDelta(dragY))
                || super.mouseDragged(x, y, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Key key = new Key(keyCode, scanCode, modifiers);

        if (wrappedGui.keyPressed(key)) {
            return true;
        } else {
            if (key.backspace()) {
                wrappedGui.onBack();
                return true;
            } else if (wrappedGui.onClosedByKey(key)) {
                if (shouldCloseOnEsc()) {
                    wrappedGui.closeGui(true);
                }
                return true;
            } else if (Platform.isModLoaded("jei")) {
                wrappedGui.getIngredientUnderMouse().ifPresent(underMouse -> handleIngredientKey(key, underMouse.ingredient()));
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        Key key = new Key(keyCode, scanCode, modifiers);
        wrappedGui.keyReleased(key);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char keyChar, int modifiers) {
        if (wrappedGui.charTyped(keyChar, new KeyModifiers(modifiers))) {
            return true;
        }

        return super.charTyped(keyChar, keyChar);
    }

    private void handleIngredientKey(Key key, Object object) {
        // Stub to keep parity with ScreenWrapper behavior.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        recomputeViewport();
        int designMouseX = toDesignMouseX(mouseX);
        int designMouseY = toDesignMouseY(mouseY);
        wrappedGui.updateGui(designMouseX, designMouseY, partialTicks);

        renderBackground(graphics, mouseX, mouseY, partialTicks);
        graphics.fill(0, 0, width, height, 0xB0111318);
        GuiHelper.setupDrawing();

        Theme theme = wrappedGui.getTheme();
        int designW = wrappedGui.designWidth();
        int designH = wrappedGui.designHeight();

        graphics.pose().pushPose();
        graphics.pose().translate(viewportX, viewportY, 0F);
        graphics.pose().scale(uiScale, uiScale, 1F);
        RealCivScreenScaleContext.activate(uiScale, viewportX, viewportY);
        try {
            wrappedGui.draw(graphics, theme, 0, 0, designW, designH);
            wrappedGui.drawForeground(graphics, theme, 0, 0, designW, designH);
        } finally {
            RealCivScreenScaleContext.clear();
            graphics.pose().popPose();
        }

        wrappedGui.addMouseOverText(tooltipList);

        int zLevel = wrappedGui.getMaxZLevel() + 100;
        graphics.pose().pushPose();
        if (!tooltipList.shouldRender()) {
            wrappedGui.getIngredientUnderMouse().ifPresent(underMouse -> {
                if (underMouse.tooltip()) {
                    Object ingredient = underMouse.ingredient();
                    if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
                        graphics.pose().translate(0, 0, zLevel);
                        graphics.renderTooltip(theme.getFont(), stack, mouseX, mouseY);
                    }
                }
            });
        } else {
            graphics.pose().translate(0, 0, zLevel);
            graphics.setColor(1F, 1F, 1F, 0.8F);
            graphics.renderTooltip(theme.getFont(), tooltipList.getLines(), Optional.empty(), mouseX, Math.max(mouseY, 18));
            graphics.setColor(1F, 1F, 1F, 1F);
        }
        graphics.pose().popPose();

        tooltipList.reset();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int x, int y, float partialTicks) {
        if (wrappedGui.drawDefaultBackground(graphics)) {
            super.renderBackground(graphics, x, y, partialTicks);
        }
    }

    @Override
    protected void renderBlurredBackground(float value) {
        if (wrappedGui.shouldRenderBlur()) {
            super.renderBlurredBackground(value);
        }
    }

    @Override
    public void tick() {
        super.tick();
        wrappedGui.tick();
    }

    @Override
    public BaseScreen getGui() {
        return wrappedGui;
    }

    @Override
    public void removed() {
        wrappedGui.setResponsiveWrapperMode(false);
        wrappedGui.onClosed();
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return wrappedGui.shouldCloseOnEsc();
    }
}
