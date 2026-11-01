package com.sakhcheese.modules.highlighter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL32;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {
	static Renderer instance = new Renderer();
	private static Minecraft mc = Minecraft.getMinecraft();

	public static boolean drawLines = true;

	@SubscribeEvent
	public void RenderWorldLastEvent(RenderWorldLastEvent event)
	{
		if (!drawLines || !com.sakhcheese.core.Reference.tracers || mc.player == null || mc.world == null)
		{
			return;
		}

		RenderManager renderManager = mc.getRenderManager();

		if (renderManager == null || renderManager.options == null) return;
		boolean firstPerson = renderManager.options.thirdPersonView == 0;

		Vec3d vec = mc.player.getPositionVector();

		double mx = vec.x;
		double mz = vec.z;
		double my = vec.y + mc.player.getEyeHeight() - 0.35;

		if (firstPerson)
		{
			double drawBeforeCameraDist = ModConfig.instance.config.getCategory(ModConfig.CATEGORY_LINES).get(ModConfig.LINES_CAMERA_DISTANCE).getDouble();
			double pitch = ((mc.player.rotationPitch + 90) * Math.PI) / 180;
			double yaw = ((mc.player.rotationYaw + 90) * Math.PI) / 180;
			mx += Math.sin(pitch) * Math.cos(yaw) * drawBeforeCameraDist;
			mz += Math.sin(pitch) * Math.sin(yaw) * drawBeforeCameraDist;
			my += Math.cos(pitch) * drawBeforeCameraDist;
		}

		enableGlConstants();

		boolean oldBobbing = mc.gameSettings.viewBobbing;
		mc.gameSettings.viewBobbing = false;

		GlStateManager.translate(-mc.player.posX, -mc.player.posY, -mc.player.posZ);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder vertexBuffer = tessellator.getBuffer();
		vertexBuffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		List<Entity> list = mc.world.getLoadedEntityList();
		// main draw loop
		for (Entity entity : list)
		{
			String entityCategory = EntityHelper.getEntityCategory(entity);

			if (entityCategory == null)
			{
				continue;
			}

			ConfigCategory configCategory = ModConfig.instance.config.getCategory(entityCategory);
			if (!configCategory.get(ModConfig.ENTITIES_ENABLE).getBoolean())
			{
				continue;
			}

			float f2 = entity.height - 0.5F - (entity.isSneaking() ? 0.25F : 0.0F);

			// distance check
			float distance = entity.getDistance(mc.player);
			float maxDistance = (float) configCategory.get(ModConfig.ENTITIES_MAX_DISTANCE).getInt();
			if (distance > maxDistance)
			{
				continue;
			}

			// vertical distance check
			// the 1.0 is a correcting factor for the fact that players count from their upper half (or something)
			// it's more than 1 actually (probably eyeHeight) but this works well as you generally want to see more stuff
			// above you than below anyway... And who cares about half-block heights and such.
			float vertDistance = Math.abs((float) ((mc.player.posY + 1.0) - entity.posY));
			if (vertDistance > (float) configCategory.get(ModConfig.ENTITIES_MAX_VERTICAL_DISTANCE).getInt())
			{
				continue;
			}

			// calculate alpha based on distance - the further the more transparent
			float alpha = 1 - (distance / (maxDistance / 2F));
			if (alpha < 0.25F)
			{
				alpha = 0.25F;
			}
			if (alpha > 1F)
			{
				alpha = 1F;
			}

			// the "draw from" pos
			vertexBuffer.pos(mx, my, mz).color(1F, 0, 0, 0).endVertex();

			// the "target" pos
			RGBColor color = EntityHelper.getRenderColor(configCategory, entity);
			vertexBuffer.pos(entity.posX, entity.posY + f2, entity.posZ).color(color.red, color.green, color.blue, alpha).endVertex();
		}

		tessellator.draw();

		mc.gameSettings.viewBobbing = oldBobbing;

		disableGlConstants();

	}

	private void enableGlConstants()
	{
		GlStateManager.pushMatrix();
		GlStateManager.disableAlpha();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableTexture2D();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		if (ModConfig.instance.config.getCategory(ModConfig.CATEGORY_LINES).get(ModConfig.LINES_SMOOTH).getBoolean()) {
			glEnable(GL_LINE_SMOOTH);
		} else {
			glDisable(GL_LINE_SMOOTH);
		}
		if (ModConfig.instance.config.getCategory(ModConfig.CATEGORY_LINES).get(ModConfig.LINES_INFINITE).getBoolean()) {
			glEnable(GL32.GL_DEPTH_CLAMP);
		}
		GlStateManager.glLineWidth((float) ModConfig.instance.config.getCategory(ModConfig.CATEGORY_LINES).get(ModConfig.LINES_WIDTH).getDouble());
	}

	private void disableGlConstants() {
		if (ModConfig.instance.config.getCategory(ModConfig.CATEGORY_LINES).get(ModConfig.LINES_SMOOTH).getBoolean()) {
			glDisable(GL_LINE_SMOOTH);
		}
		if (ModConfig.instance.config.getCategory(ModConfig.CATEGORY_LINES).get(ModConfig.LINES_INFINITE).getBoolean()) {
			glDisable(GL32.GL_DEPTH_CLAMP);
		}
		GlStateManager.enableTexture2D();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.popMatrix();
	}
}
