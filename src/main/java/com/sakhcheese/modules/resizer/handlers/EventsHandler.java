package com.sakhcheese.modules.resizer.handlers;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import com.sakhcheese.modules.resizer.props.Size;

public class EventsHandler {
    private final Deque<EntityLivingBase> scaled = new ArrayDeque<>();
    private EntityPlayer eyePlayer;
    private float originalEyeHeight;

    @SubscribeEvent
    public void onCameraSetup(CameraSetup event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || Size.y <= 1) return;
        if (mc.gameSettings.thirdPersonView == 1) GlStateManager.translate(0, 0, -Size.y * 2);
        if (mc.gameSettings.thirdPersonView == 2) GlStateManager.translate(0, 0, Size.y * 2);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityRenderPre(RenderLivingEvent.Pre event) {
        if (!(event.getEntity() instanceof EntityPlayer) || Size.isDefault()) return;
        Size.set(Size.x, Size.y, Size.z);
        GlStateManager.pushMatrix();
        scaled.push(event.getEntity());
        GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        GlStateManager.scale(Size.x, Size.y, Size.z);
        GlStateManager.translate(-event.getX(), -event.getY(), -event.getZ());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingRenderPost(RenderLivingEvent.Post event) {
        if (!scaled.isEmpty() && scaled.peek() == event.getEntity()) {
            scaled.pop();
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.END
                || event.player != Minecraft.getMinecraft().player) return;
        if (eyePlayer != event.player) restoreEyeHeight();
        if (Size.isDefault()) {
            restoreEyeHeight();
        } else {
            if (eyePlayer == null) {
                eyePlayer = event.player;
                originalEyeHeight = eyePlayer.eyeHeight;
            }
            Size.set(Size.x, Size.y, Size.z);
            eyePlayer.eyeHeight = originalEyeHeight * Size.y;
        }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            restoreEyeHeight();
            Size.reset();
        }
    }

    private void restoreEyeHeight() {
        if (eyePlayer != null) eyePlayer.eyeHeight = originalEyeHeight;
        eyePlayer = null;
    }
}
