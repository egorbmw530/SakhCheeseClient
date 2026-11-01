package com.sakhcheese.hud;

import com.sakhcheese.core.Reference;
import com.sakhcheese.modules.autosprint.AutoSprint;
import com.sakhcheese.modules.freecam.FreerCamForge;
import com.sakhcheese.modules.hitboxes.Hitboxes;
import com.sakhcheese.modules.resizer.props.Size;
import com.sakhcheese.modules.spammer.PropsUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Compact upper-left status line and short-lived messages owned by this client. */
public final class SystemOverlay {
    private static final long MESSAGE_LIFETIME = 10_000_000_000L;
    private static final int MAX_MESSAGES = 5;
    private static final Deque<Message> MESSAGES = new ArrayDeque<>();

    public static void post(String text) {
        if (text == null) return;
        String clean = text.trim();
        if (clean.isEmpty()) return;
        if (clean.length() > 220) clean = clean.substring(0, 220);
        synchronized (MESSAGES) {
            while (MESSAGES.size() >= MAX_MESSAGES) MESSAGES.removeFirst();
            MESSAGES.addLast(new Message(clean, System.nanoTime() + MESSAGE_LIFETIME));
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.gameSettings.hideGUI) return;
        FontRenderer font = mc.fontRenderer;
        ScaledResolution resolution = new ScaledResolution(mc);
        int wrapWidth = Math.max(120, Math.min(260, resolution.getScaledWidth() / 2));
        List<String> lines = new ArrayList<>();
        String states = activeStates();
        if (!states.isEmpty()) lines.add("§e" + states);
        long now = System.nanoTime();
        synchronized (MESSAGES) {
            while (!MESSAGES.isEmpty() && now - MESSAGES.peekFirst().expiresAt >= 0) MESSAGES.removeFirst();
            for (Message message : MESSAGES) lines.addAll(font.listFormattedStringToWidth("§f" + message.text, wrapWidth));
        }
        if (lines.isEmpty()) return;
        int width = 0;
        for (String line : lines) width = Math.max(width, font.getStringWidth(line));
        int lineHeight = font.FONT_HEIGHT + 2;
        // The whole window, including its font and padding, is 2.5 times smaller.
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(0.4F, 0.4F, 1.0F);
            Gui.drawRect(5, 5, 11 + width, 9 + lines.size() * lineHeight, 0xA0000000);
            int y = 8;
            for (String line : lines) {
                font.drawStringWithShadow(line, 8, y, 0xFFFFFF);
                y += lineHeight;
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) return;
        synchronized (MESSAGES) { MESSAGES.clear(); }
    }

    public static String activeStates() {
        List<String> states = new ArrayList<>();
        if (Reference.BitBoxes) states.add(String.format(Locale.ROOT, "HB-%.1f", Hitboxes.customWidth));
        if (FreerCamForge.isActive()) states.add("C");
        if (AutoSprint.isEnabled()) states.add("S");
        if (Reference.fullBright) states.add("B");
        if (Reference.tracers) states.add("T");
        if (Reference.trajectory) states.add("J");
        if (Reference.parkour) states.add("P");
        if (Reference.glower) states.add("G");
        if (Reference.zamorozka) states.add("O");
        if (Reference.autoAnswer) states.add("A");
        if (Reference.sellFish) states.add("R");
        if (!Size.isDefault()) states.add("Z");
        if (PropsUtil.isActive) states.add("M");
        return String.join(" ", states);
    }

    private static final class Message {
        final String text;
        final long expiresAt;
        Message(String text, long expiresAt) { this.text = text; this.expiresAt = expiresAt; }
    }
}
