package com.sakhcheese.gui;

/** Keeps the original two-column layout within the current Minecraft GUI area. */
public final class MenuLayout {
    public final int width, height;
    public final float scaleX, scaleY;

    public MenuLayout(int screenWidth, int screenHeight, int minimumWidth, int minimumHeight) {
        double scale = Math.min(1.0, Math.min(screenWidth / (double) minimumWidth, screenHeight / (double) minimumHeight));
        scale = Math.max(0.01, scale);
        width = (int) Math.ceil(screenWidth / scale);
        height = (int) Math.ceil(screenHeight / scale);
        scaleX = screenWidth / (float) width;
        scaleY = screenHeight / (float) height;
    }

    public int mouseX(int x) { return (int) (x / scaleX); }
    public int mouseY(int y) { return (int) (y / scaleY); }
}
