package com.sakhcheese.gui;

public final class MenuLayoutChecks {
    public static void main(String[] args) {
        int[][] sizes = {{320, 240}, {427, 240}, {640, 360}, {1920, 1080}, {240, 320}, {160, 90}};
        for (int[] size : sizes) {
            MenuLayout layout = new MenuLayout(size[0], size[1], 420, 300);
            check(layout.width >= 420 && layout.height >= 300, "minimum usable canvas");
            int left = layout.width / 2 - 200, right = layout.width / 2 + 200;
            int top = layout.height - layout.height / 4 - 190;
            int bottom = layout.height - layout.height / 4 + 40;
            check(left >= 0 && right <= layout.width && top >= 0 && bottom <= layout.height, "controls within canvas");
            check(Math.abs(layout.width * layout.scaleX - size[0]) < 0.01, "render width");
            check(Math.abs(layout.height * layout.scaleY - size[1]) < 0.01, "render height");
            check(Math.abs(layout.mouseX(size[0] / 2) - layout.width / 2) <= 2, "mouse center X");
            check(Math.abs(layout.mouseY(size[1] / 2) - layout.height / 2) <= 2, "mouse center Y");
        }
        MenuLayout normal = new MenuLayout(640, 360, 420, 300);
        check(normal.scaleX == 1 && normal.scaleY == 1, "normal screens preserve size");
        System.out.println("Menu layout: 37 checks passed");
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
