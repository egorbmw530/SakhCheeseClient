package com.sakhcheese.modules.freecam;

import com.sakhcheese.input.KeybindsRegister;
import com.mojang.authlib.GameProfile;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/** Independent client camera. The player and network connection remain in their normal state. */
public class FreerCamForge {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static boolean active;
    private static FreerCamForge activeInstance;
    private EntityOtherPlayerMP camera;
    private EntityOtherPlayerMP bodyModel;
    private Entity previousView;
    private float savedYaw;
    private float savedPitch;
    private float savedPrevYaw;
    private float savedPrevPitch;
    private float pendingYawDelta;
    private float pendingPitchDelta;
    private boolean lookSnapshot;
    private boolean lookCaptured;
    private boolean lookTransferred;
    private RayTraceResult visibleTarget;
    private World visibleTargetWorld;
    private boolean visibleTargetReady;
    private final FloatBuffer modelViewMatrix = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer nearPoint = BufferUtils.createFloatBuffer(3);
    private final FloatBuffer farPoint = BufferUtils.createFloatBuffer(3);

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!KeybindsRegister.FreeCamForge.isPressed()) return;
        if (camera != null) disable();
        else if (mc.player != null && mc.world != null && mc.currentScreen == null) {
            previousView = mc.getRenderViewEntity();
            camera = new EntityOtherPlayerMP(mc.world, new GameProfile(UUID.randomUUID(), "Freecam"));
            camera.copyLocationAndAnglesFrom(mc.player);
            camera.prevPosX = camera.lastTickPosX = camera.posX;
            camera.prevPosY = camera.lastTickPosY = camera.posY;
            camera.prevPosZ = camera.lastTickPosZ = camera.posZ;
            camera.noClip = true;
            bodyModel = new VisualBody(mc.world, mc.player.getGameProfile());
            syncCameraState();
            syncBodyModel();
            mc.setRenderViewEntity(camera);
            active = true;
            activeInstance = this;
            visibleTarget = null;
            visibleTargetWorld = mc.world;
            visibleTargetReady = false;
            com.sakhcheese.hud.SystemOverlay.post("FreeCam включён: тело игрока остаётся в мире.");
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (camera == null || mc.player == null) return;
        if (event.phase == TickEvent.Phase.START) {
            savedYaw = mc.player.rotationYaw;
            savedPitch = mc.player.rotationPitch;
            savedPrevYaw = mc.player.prevRotationYaw;
            savedPrevPitch = mc.player.prevRotationPitch;
            lookSnapshot = true;
            lookCaptured = false;
            lookTransferred = false;
        } else {
            applyCapturedLookToCamera();
            lookSnapshot = false;
        }
    }

    /** Restore the real player before its model is drawn. Mouse look is transferred to the camera only. */
    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Pre event) {
        if (camera != null && event.getEntityPlayer() == mc.player) {
            captureLookAndRestoreBody();
            // The explicit client-only copy below represents the body while the view is detached.
            if (bodyModel != null) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (camera == null || mc.player == null || mc.world != camera.world) return;
        // The world was rendered with the camera's current angles. Capture the next
        // mouse delta for the following frame, but do not change this frame's ray.
        captureLookAndRestoreBody();
        // Cache exactly what the player sees in this rendered frame. Input events happen
        // between frames, so recomputing here and again on click can produce two rays.
        RayTraceResult target = traceFromRenderedView(128.0D, event.getPartialTicks());
        visibleTarget = target;
        visibleTargetWorld = mc.world;
        visibleTargetReady = true;
        if (target != null && target.typeOfHit == RayTraceResult.Type.BLOCK) {
            // Vanilla objectMouseOver can still be calculated from the real player during
            // the detached-camera handoff. Draw the exact ray target used by AutoParkour.
            mc.renderGlobal.drawSelectionBox(camera, target, 0, event.getPartialTicks());
        }
        if (bodyModel == null || mc.world != bodyModel.world) return;
        // At activation the detached camera is inside the body model. Rendering its inner
        // faces looks like a full-screen color filter, so show the model after flying away.
        if (camera.getDistanceSq(mc.player) < 2.25D) return;
        syncBodyModel();
        float brightnessX = OpenGlHelper.lastBrightnessX;
        float brightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        try {
            mc.getRenderManager().renderEntityStatic(bodyModel, event.getPartialTicks(), false);
        } finally {
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onHandRender(RenderHandEvent event) {
        // A detached camera has no hand. Suppressing it also prevents first-person arm jitter.
        if (camera != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onMouse(net.minecraftforge.client.event.MouseEvent event) {
        if (camera != null && mc.currentScreen == null && event.getButton() >= 0) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onInput(InputUpdateEvent event) {
        if (camera == null || event.getEntityPlayer() != mc.player) return;
        event.getMovementInput().moveForward = 0;
        event.getMovementInput().moveStrafe = 0;
        event.getMovementInput().jump = false;
        event.getMovementInput().sneak = false;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || camera == null) return;
        if (mc.player == null || mc.world != camera.world || mc.player.isDead) {
            disable();
            return;
        }
        camera.lastTickPosX = camera.prevPosX = camera.posX;
        camera.lastTickPosY = camera.prevPosY = camera.posY;
        camera.lastTickPosZ = camera.prevPosZ = camera.posZ;
        camera.prevRotationYaw = camera.rotationYaw;
        camera.prevRotationPitch = camera.rotationPitch;
        syncCameraState();
        syncBodyModel();
        if (mc.currentScreen != null || !mc.inGameHasFocus) return;
        double forward = down(mc.gameSettings.keyBindForward.getKeyCode()) - down(mc.gameSettings.keyBindBack.getKeyCode());
        double strafe = down(mc.gameSettings.keyBindLeft.getKeyCode()) - down(mc.gameSettings.keyBindRight.getKeyCode());
        double vertical = down(mc.gameSettings.keyBindJump.getKeyCode()) - down(mc.gameSettings.keyBindSneak.getKeyCode());
        double yaw = Math.toRadians(camera.rotationYaw);
        Vec3d movement = new Vec3d(-Math.sin(yaw) * forward + Math.cos(yaw) * strafe,
                vertical, Math.cos(yaw) * forward + Math.sin(yaw) * strafe);
        if (movement.lengthSquared() > 0) {
            movement = movement.normalize().scale(down(mc.gameSettings.keyBindSprint.getKeyCode()) > 0 ? 1.0 : 0.4);
            camera.setPosition(camera.posX + movement.x, camera.posY + movement.y, camera.posZ + movement.z);
        }
    }

    private int down(int key) {
        if (key < 0) return org.lwjgl.input.Mouse.isButtonDown(key + 100) ? 1 : 0;
        return key > 0 && key < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(key) ? 1 : 0;
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) disable();
    }

    private void disable() {
        if (camera == null) return;
        applyCapturedLookToCamera();
        mc.setRenderViewEntity(previousView != null && previousView.world == mc.world ? previousView : mc.player);
        camera = null;
        bodyModel = null;
        previousView = null;
        lookSnapshot = false;
        active = false;
        visibleTarget = null;
        visibleTargetWorld = null;
        visibleTargetReady = false;
        if (activeInstance == this) activeInstance = null;
        com.sakhcheese.hud.SystemOverlay.post("FreeCam выключен.");
    }

    private void captureLookAndRestoreBody() {
        if (!lookSnapshot || camera == null || mc.player == null) return;
        if (!lookCaptured) {
            pendingYawDelta = MathHelper.wrapDegrees(mc.player.rotationYaw - savedYaw);
            pendingPitchDelta = mc.player.rotationPitch - savedPitch;
            lookCaptured = true;
        }
        mc.player.rotationYaw = savedYaw;
        mc.player.rotationPitch = savedPitch;
        mc.player.prevRotationYaw = savedPrevYaw;
        mc.player.prevRotationPitch = savedPrevPitch;
    }

    private void applyCapturedLookToCamera() {
        captureLookAndRestoreBody();
        if (!lookCaptured || lookTransferred || camera == null) return;
        camera.rotationYaw += pendingYawDelta;
        camera.rotationPitch = MathHelper.clamp(camera.rotationPitch + pendingPitchDelta, -90.0F, 90.0F);
        lookTransferred = true;
    }

    private void syncBodyModel() {
        if (bodyModel == null || mc.player == null) return;
        bodyModel.lastTickPosX = mc.player.lastTickPosX;
        bodyModel.lastTickPosY = mc.player.lastTickPosY;
        bodyModel.lastTickPosZ = mc.player.lastTickPosZ;
        bodyModel.prevPosX = mc.player.prevPosX;
        bodyModel.prevPosY = mc.player.prevPosY;
        bodyModel.prevPosZ = mc.player.prevPosZ;
        bodyModel.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ);
        bodyModel.rotationYaw = mc.player.rotationYaw;
        bodyModel.rotationPitch = mc.player.rotationPitch;
        bodyModel.prevRotationYaw = mc.player.prevRotationYaw;
        bodyModel.prevRotationPitch = mc.player.prevRotationPitch;
        bodyModel.rotationYawHead = mc.player.rotationYawHead;
        bodyModel.prevRotationYawHead = mc.player.prevRotationYawHead;
        bodyModel.renderYawOffset = mc.player.renderYawOffset;
        bodyModel.prevRenderYawOffset = mc.player.prevRenderYawOffset;
        bodyModel.limbSwing = mc.player.limbSwing;
        bodyModel.limbSwingAmount = mc.player.limbSwingAmount;
        bodyModel.prevLimbSwingAmount = mc.player.prevLimbSwingAmount;
        bodyModel.setSneaking(mc.player.isSneaking());
        bodyModel.setSprinting(mc.player.isSprinting());
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack source = mc.player.getItemStackFromSlot(slot);
            ItemStack shown = bodyModel.getItemStackFromSlot(slot);
            if (!ItemStack.areItemStacksEqual(source, shown)) {
                bodyModel.setItemStackToSlot(slot, source.isEmpty() ? ItemStack.EMPTY : source.copy());
            }
        }
    }

    /** Keep visual effects/FOV identical to the real player while the view entity is detached. */
    private void syncCameraState() {
        if (camera == null || mc.player == null) return;
        camera.capabilities.allowEdit = mc.player.capabilities.allowEdit;
        camera.capabilities.allowFlying = mc.player.capabilities.allowFlying;
        camera.capabilities.disableDamage = mc.player.capabilities.disableDamage;
        camera.capabilities.isCreativeMode = mc.player.capabilities.isCreativeMode;
        camera.capabilities.isFlying = mc.player.capabilities.isFlying;
        camera.capabilities.setFlySpeed(mc.player.capabilities.getFlySpeed());
        camera.capabilities.setPlayerWalkSpeed(mc.player.capabilities.getWalkSpeed());
        camera.setHealth(mc.player.getHealth());
        camera.clearActivePotions();
        for (PotionEffect effect : mc.player.getActivePotionEffects()) {
            camera.addPotionEffect(new PotionEffect(effect));
        }
    }

    public boolean isEnabled() { return camera != null; }
    public static boolean isActive() { return active; }

    /** Ray used by FreeCam tools. It never falls back to the real player's eyes. */
    public static RayTraceResult rayTraceFromCamera(double reach) {
        FreerCamForge instance = activeInstance;
        if (instance == null || instance.camera == null) return null;
        // Use the block that was outlined in the last completed frame. This guarantees
        // that a click cannot jump through it because of a newer, not-yet-rendered turn.
        if (instance.visibleTargetReady && instance.visibleTargetWorld == instance.mc.world) {
            return instance.visibleTarget;
        }
        return instance.traceFromCamera(reach, 1.0F);
    }

    private RayTraceResult traceFromCamera(double reach, float partialTicks) {
        if (camera == null || mc.world == null) return null;
        Vec3d start = camera.getPositionEyes(partialTicks);
        Vec3d direction = camera.getLook(partialTicks).normalize();
        return traceFirstBlockVoxel(mc.world, start, direction, reach);
    }

    /** Build the ray from the matrices that produced the current framebuffer. */
    private RayTraceResult traceFromRenderedView(double reach, float partialTicks) {
        if (camera == null || mc.world == null) return null;
        modelViewMatrix.clear();
        projectionMatrix.clear();
        viewport.clear();
        nearPoint.clear();
        farPoint.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        modelViewMatrix.rewind();
        projectionMatrix.rewind();
        viewport.rewind();
        float centerX = viewport.get(0) + viewport.get(2) * 0.5F;
        float centerY = viewport.get(1) + viewport.get(3) * 0.5F;
        if (!GLU.gluUnProject(centerX, centerY, 0.0F, modelViewMatrix, projectionMatrix, viewport, nearPoint)) {
            return traceFromCamera(reach, partialTicks);
        }
        modelViewMatrix.rewind();
        projectionMatrix.rewind();
        viewport.rewind();
        if (!GLU.gluUnProject(centerX, centerY, 1.0F, modelViewMatrix, projectionMatrix, viewport, farPoint)) {
            return traceFromCamera(reach, partialTicks);
        }

        // World rendering in 1.12.2 supplies geometry relative to the interpolated
        // render-view entity. Convert the unprojected camera-relative points back.
        double baseX = camera.prevPosX + (camera.posX - camera.prevPosX) * partialTicks;
        double baseY = camera.prevPosY + (camera.posY - camera.prevPosY) * partialTicks;
        double baseZ = camera.prevPosZ + (camera.posZ - camera.prevPosZ) * partialTicks;
        Vec3d start = new Vec3d(nearPoint.get(0) + baseX, nearPoint.get(1) + baseY, nearPoint.get(2) + baseZ);
        Vec3d end = new Vec3d(farPoint.get(0) + baseX, farPoint.get(1) + baseY, farPoint.get(2) + baseZ);
        Vec3d direction = end.subtract(start).normalize();
        return traceFirstBlockVoxel(mc.world, start, direction, reach);
    }

    /**
     * Select the first non-air block cell crossed by the camera ray. Vanilla collision
     * ray tracing can pass through the empty portion of fences, slabs or open trapdoors;
     * waypoint selection deliberately treats every occupied block cell as clickable
     * from all six directions.
     */
    static RayTraceResult traceFirstBlockVoxel(World world, Vec3d start, Vec3d direction, double reach) {
        if (world == null || start == null || direction == null || reach <= 0
                || !Double.isFinite(start.x + start.y + start.z + direction.x + direction.y + direction.z + reach)
                || direction.lengthSquared() < 1.0E-12D) return null;

        int x = MathHelper.floor(start.x);
        int y = MathHelper.floor(start.y);
        int z = MathHelper.floor(start.z);
        int stepX = direction.x > 0 ? 1 : direction.x < 0 ? -1 : 0;
        int stepY = direction.y > 0 ? 1 : direction.y < 0 ? -1 : 0;
        int stepZ = direction.z > 0 ? 1 : direction.z < 0 ? -1 : 0;
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(direction.x);
        double deltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(direction.y);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0D / Math.abs(direction.z);
        double nextX = stepX > 0 ? (x + 1.0D - start.x) * deltaX
                : stepX < 0 ? (start.x - x) * deltaX : Double.POSITIVE_INFINITY;
        double nextY = stepY > 0 ? (y + 1.0D - start.y) * deltaY
                : stepY < 0 ? (start.y - y) * deltaY : Double.POSITIVE_INFINITY;
        double nextZ = stepZ > 0 ? (z + 1.0D - start.z) * deltaZ
                : stepZ < 0 ? (start.z - z) * deltaZ : Double.POSITIVE_INFINITY;
        EnumFacing enteredFace = oppositeDominant(direction);
        double distance = 0.0D;
        int maxSteps = Math.max(8, (int) Math.ceil(reach * 3.0D) + 6);

        for (int steps = 0; steps < maxSteps && distance <= reach; steps++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (y >= 0 && y < 256) {
                if (!world.isBlockLoaded(pos)) return null;
                if (!world.isAirBlock(pos)) {
                    Vec3d hit = start.addVector(direction.x * distance, direction.y * distance,
                            direction.z * distance);
                    return new RayTraceResult(hit, enteredFace, pos);
                }
            }

            if (nextX <= nextY && nextX <= nextZ) {
                distance = nextX;
                nextX += deltaX;
                x += stepX;
                enteredFace = stepX > 0 ? EnumFacing.WEST : EnumFacing.EAST;
            } else if (nextY <= nextZ) {
                distance = nextY;
                nextY += deltaY;
                y += stepY;
                enteredFace = stepY > 0 ? EnumFacing.DOWN : EnumFacing.UP;
            } else {
                distance = nextZ;
                nextZ += deltaZ;
                z += stepZ;
                enteredFace = stepZ > 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
            }
        }
        return null;
    }

    private static EnumFacing oppositeDominant(Vec3d direction) {
        double ax = Math.abs(direction.x), ay = Math.abs(direction.y), az = Math.abs(direction.z);
        if (ax >= ay && ax >= az) return direction.x >= 0 ? EnumFacing.WEST : EnumFacing.EAST;
        if (ay >= az) return direction.y >= 0 ? EnumFacing.DOWN : EnumFacing.UP;
        return direction.z >= 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
    }

    /** Detached render data only: never spawned, targetable, collidable, pushable or damageable. */
    private static final class VisualBody extends EntityOtherPlayerMP {
        VisualBody(World world, GameProfile profile) {
            super(world, profile);
            noClip = true;
            setEntityInvulnerable(true);
        }

        @Override public boolean canBeCollidedWith() { return false; }
        @Override public boolean canBePushed() { return false; }
        @Override public AxisAlignedBB getCollisionBox(Entity entityIn) { return null; }
        @Override public AxisAlignedBB getCollisionBoundingBox() { return null; }
        @Override public void applyEntityCollision(Entity entityIn) { }
        @Override public boolean attackEntityFrom(DamageSource source, float amount) { return false; }
    }
}
