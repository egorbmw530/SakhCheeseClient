package com.sakhcheese.modules.autosprint;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SprintHolder {
    private final AutoSprint asInstance;
    private EntityPlayer controlled;

    public SprintHolder(AutoSprint asInstance) { this.asInstance = asInstance; }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (controlled != null && controlled != mc.player) disable();
        boolean sprint = asInstance.getState() && mc.player != null && mc.world != null
                && mc.currentScreen == null && mc.inGameHasFocus && mc.getRenderViewEntity() == mc.player
                && !com.sakhcheese.modules.parkour.AutoParkour.isControlling()
                && !mc.player.isSneaking() && !mc.player.collidedHorizontally && !mc.player.isHandActive()
                && !mc.player.isRiding() && !mc.player.isElytraFlying() && !mc.player.isDead
                && mc.player.movementInput.moveForward >= 0.8F
                && (mc.player.getFoodStats().getFoodLevel() > 6 || mc.player.capabilities.allowFlying);
        if (!sprint) { disable(); return; }
        if (!mc.player.isSprinting()) {
            mc.player.setSprinting(true);
            controlled = mc.player;
        }
    }

    public void disable() {
        if (controlled != null) controlled.setSprinting(false);
        controlled = null;
    }
}
