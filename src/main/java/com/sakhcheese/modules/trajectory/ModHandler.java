// 
// 

package com.sakhcheese.modules.trajectory;

import com.sakhcheese.core.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.init.Items;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;

public class ModHandler {
    public static float getArrowVelocity(final int charge) {
        float f = charge / 20.0f;
        f = (f * f + f * 2.0f) / 3.0f;
        if (f > 1.0f) {
            f = 1.0f;
        }
        return f;
    }

    public List<Vec3d> getPositions(final Vec3d playerPos) {

        final List<Vec3d> positions = new ArrayList<Vec3d>();
        final EntityPlayerSP player = Minecraft.getMinecraft().player;
        double motionX = -MathHelper.sin(player.rotationYaw * 0.017453292f) * MathHelper.cos(player.rotationPitch * 0.017453292f);
        double motionY = -MathHelper.sin(player.rotationPitch * 0.017453292f);
        double motionZ = MathHelper.cos(player.rotationYaw * 0.017453292f) * MathHelper.cos(player.rotationPitch * 0.017453292f);
        double posX = playerPos.x;
        double posY = playerPos.y + player.getEyeHeight() - 0.10000000149011612;
        double posZ = playerPos.z;
        double velocity = 1.5;
        if (player.getItemInUseCount() != 0) {
            velocity = getArrowVelocity(72000 - player.getItemInUseCount()) * 3.0f;
        }
        final double f = MathHelper.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= f;
        motionY /= f;
        motionZ /= f;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        int count = 1000;
        int i = 0;
        while (new Vec3d(posX, posY, posZ).distanceTo(playerPos) < Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16 * 8) {
            for (int j = 0; j < count; ++j) {
                positions.add(new Vec3d(posX + motionX / count * j, posY + motionY / count * j, posZ + motionZ / count * j));
            }
            count /= 3;
            if (count < 1) {
                count = 1;
            }
            final Vec3d vec3d1 = new Vec3d(posX, posY, posZ);
            final Vec3d vec3d2 = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
            final RayTraceResult rayTraceResult = Minecraft.getMinecraft().world.rayTraceBlocks(vec3d1, vec3d2, false, true, false);
            if (rayTraceResult != null) {
                return positions;
            }
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
            if (player.getItemInUseCount() != 0) {
                motionY -= 0.05;
            } else {
                motionY -= 0.03;
            }
            ++i;
        }

        return positions;

    }

    @SubscribeEvent
    public void renderWorldLastEvent(final RenderWorldLastEvent event) {
        if (Reference.trajectory) {
            GL11.glPushMatrix();
            final EntityPlayerSP player = Minecraft.getMinecraft().player;
            final double playerPosX = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
            final double playerPosY = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks();
            final double playerPosZ = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();
            GL11.glTranslated(-playerPosX, -playerPosY, -playerPosZ);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(3553);
            GL11.glDisable(2884);
            GL11.glBegin(7);
            if (player.getHeldItem(EnumHand.MAIN_HAND) != null && ((player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.BOW && player.getItemInUseCount() != 0) || player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.ENDER_PEARL || player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.EGG || player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.EGG)) {
                final List<Vec3d> positions = this.getPositions(new Vec3d(playerPosX, playerPosY, playerPosZ));
                for (int i = positions.size() - 2; i >= 0; --i) {
                    GL11.glColor4d(i * 1.0 / positions.size(), 1.0 - i * 1.0 / positions.size(), 0.0, 0.5);
                    final Vec3d vec1 = positions.get(i);
                    final Vec3d vec2 = positions.get(i + 1);
                    final double angle = Math.toRadians(player.rotationYaw + (player.rotationYaw - player.prevRotationYaw) * event.getPartialTicks());
                    final double size = 0.1;
                    final double xOffset = Math.cos(angle) * size;
                    final double zOffset = Math.sin(angle) * size;
                    GL11.glVertex3d(vec1.x - xOffset, vec1.y, vec1.z - zOffset);
                    GL11.glVertex3d(vec1.x + xOffset, vec1.y, vec1.z + zOffset);
                    GL11.glVertex3d(vec2.x + xOffset, vec2.y, vec2.z + zOffset);
                    GL11.glVertex3d(vec2.x - xOffset, vec2.y, vec2.z - zOffset);
                }
            }
            GL11.glEnd();
            GL11.glEnable(2884);
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }
    }

    @SubscribeEvent
    public void renderGameOverlayEvent(final RenderGameOverlayEvent.Post event) {
        if (Reference.trajectory) {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
                final int scaledWidth = scaledResolution.getScaledWidth();
                final int scaledHeight = scaledResolution.getScaledHeight();
                final EntityPlayerSP player = Minecraft.getMinecraft().player;
                if (player.getHeldItem(EnumHand.MAIN_HAND) != null && (player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.BOW || player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.ENDER_PEARL || player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.EGG || player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.SNOWBALL)) {
                    if (player.getItemInUseCount() != 0) {
                        final float value = getArrowVelocity(72000 - player.getItemInUseCount());
                        final String text = String.valueOf((int) (value * 100.0f));
                        final int color = (((int) (255.0f - value * 255.0f) & 0xFF) << 16) + (((int) (value * 255.0f) & 0xFF) << 8);
                        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text + "%", (float) (scaledWidth / 2 - Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2), (float) (scaledHeight / 2 + 20), color);
                    }
                    int count = 0;

                    final String text = String.valueOf(count);
                    float value2 = 1.0f;
                    if (count < 64 && player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.BOW) {
                        value2 = count / 64.0f;
                    }
                    if (count < 16 && player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.ENDER_PEARL) {
                        value2 = count / 16.0f;
                    }
                    if (count < 16 && player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.EGG) {
                        value2 = count / 16.0f;
                    }
                    if (count < 16 && player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.SNOWBALL) {
                        value2 = count / 16.0f;
                    }
                    final int color2 = (((int) (255.0f - value2 * 255.0f) & 0xFF) << 16) + (((int) (value2 * 255.0f) & 0xFF) << 8);
                    Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, scaledWidth / 2 - Minecraft.getMinecraft().fontRenderer.getStringWidth(text) / 2.0f, (float) (scaledHeight / 2 - 20), color2);
                }
            }
        }
    }
}
