package com.sakhcheese.gui;

import com.sakhcheese.core.Reference;
import com.sakhcheese.modules.hitboxes.Hitboxes;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.GuiSlider;

public class GuiHitboxes extends GuiScreen {
    private final GuiScreen parent;
    private GuiButton toggle;
    private GuiSlider widthSlider;
    private MenuLayout layout;

    public GuiHitboxes(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        buttonList.clear();
        layout = new MenuLayout(width, height, 220, 180);
        width = layout.width;
        height = layout.height;
        int x = width / 2 - 100;
        int y = height / 2 - 45;
        toggle = new GuiButton(0, x, y, label());
        widthSlider = new GuiSlider(1, x, y + 25, 200, 20, "Увеличение: +", "", 0, 4.9, Hitboxes.customWidth, true, true);
        buttonList.add(toggle);
        buttonList.add(widthSlider);
        buttonList.add(new GuiButton(2, x, y + 50, "Сбросить значения"));
        buttonList.add(new GuiButton(3, x, y + 75, "Назад"));
    }

    private String label() { return "Хитбоксы " + (Reference.BitBoxes ? "§2On" : "§4Off"); }

    @Override
    public void updateScreen() { Hitboxes.setWidth((float) widthSlider.getValue()); }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            Hitboxes.setEnabled(!Reference.BitBoxes);
            Hitboxes.instance.save();
            toggle.displayString = label();
            com.sakhcheese.hud.SystemOverlay.post(Reference.BitBoxes
                    ? String.format(java.util.Locale.ROOT, "Хитбоксы включены: HB-%.1f.", Hitboxes.customWidth)
                    : "Хитбоксы выключены.");
        }
        if (button.id == 2) {
            Hitboxes.setEnabled(false);
            Hitboxes.setWidth(0);
            widthSlider.setValue(0);
            widthSlider.updateSlider();
            toggle.displayString = label();
            Hitboxes.instance.save();
            com.sakhcheese.hud.SystemOverlay.post("Хитбоксы сброшены.");
        }
        if (button.id == 3) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        Hitboxes.setWidth((float) widthSlider.getValue());
        Hitboxes.instance.save();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) mc.displayGuiScreen(parent);
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        try {
            net.minecraft.client.renderer.GlStateManager.scale(layout.scaleX, layout.scaleY, 1);
            drawDefaultBackground();
            drawCenteredString(fontRenderer, "Хитбоксы", width / 2, height / 2 - 70, 0xFFFFFF);
            super.drawScreen(layout.mouseX(mouseX), layout.mouseY(mouseY), partialTicks);
        } finally {
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
