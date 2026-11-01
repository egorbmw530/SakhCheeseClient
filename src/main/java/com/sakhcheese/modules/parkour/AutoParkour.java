package com.sakhcheese.modules.parkour;

import com.sakhcheese.core.Reference;
import com.sakhcheese.input.KeybindsRegister;
import com.sakhcheese.hud.SystemOverlay;
import com.sakhcheese.modules.freecam.FreerCamForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AutoParkour {
    private static final Logger LOGGER = LogManager.getLogger("SakhCheeseClient/AutoParkour");
    private final Minecraft mc = Minecraft.getMinecraft();
    // Keep the original description so existing P remaps survive in options.txt.
    private final KeyBinding select = new KeyBinding("AutoParkour: выбрать блок / отменить", Keyboard.KEY_P, KeybindsRegister.CATEGORY);
    private final KeyBinding start = new KeyBinding("AutoParkour: запустить / остановить маршрут", Keyboard.KEY_O, KeybindsRegister.CATEGORY);
    private final KeyBinding undo = new KeyBinding("AutoParkour: удалить точку (Ctrl — очистить)", Keyboard.KEY_BACK, KeybindsRegister.CATEGORY);
    private final ParkourRoute<BlockPos> route = new ParkourRoute<>();
    private static boolean controlling;
    private JumpPlanner.Plan plan;
    private BlockPos target;
    private World world;
    private int ticks, waitTicks;
    private boolean launched;
    private double startX, startZ;
    private double landingY;
    private int failedPoint = -1;
    private int lastMouseSelectionTick = Integer.MIN_VALUE;

    public AutoParkour() {
        ClientRegistry.registerKeyBinding(select);
        ClientRegistry.registerKeyBinding(start);
        ClientRegistry.registerKeyBinding(undo);
    }

    public static boolean isControlling() { return controlling; }

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        boolean add = select.isPressed(), run = start.isPressed(), remove = undo.isPressed();
        if (!Reference.parkour || mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (world != null && world != mc.world) clear();
        if (remove) {
            stop();
            failedPoint = -1;
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                clear(); message("Маршрут очищен.");
            } else if (route.undo()) message("Последняя точка удалена. Осталось: " + route.points().size());
            return;
        }
        if (run) {
            if (route.isRunning()) { stop(); message("Маршрут остановлен. Оставшиеся точки сохранены."); }
            else startRoute();
            return;
        }
        if (!add) return;
        selectPoint();
    }

    /** Left click selects a waypoint in FreeCam while vanilla world interaction stays canceled. */
    @SubscribeEvent(receiveCanceled = true)
    public void mouse(MouseEvent event) {
        if (!event.isButtonstate() || event.getButton() != 0 || !FreerCamForge.isActive()
                || !Reference.parkour || mc.player == null || mc.world == null || mc.currentScreen != null) return;
        event.setCanceled(true);
        // Some LWJGL/Forge combinations can expose the same press more than once in a tick.
        if (lastMouseSelectionTick == mc.player.ticksExisted) return;
        lastMouseSelectionTick = mc.player.ticksExisted;
        selectPoint();
    }

    private void selectPoint() {
        if (route.isRunning()) { stop(); message("Маршрут остановлен."); return; }
        // The hit block is used regardless of which of its six faces the ray touches.
        double reach = FreerCamForge.isActive() ? 128.0D : 6.0D;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;
        RayTraceResult hit = FreerCamForge.isActive()
                ? FreerCamForge.rayTraceFromCamera(reach)
                : view.rayTrace(reach, 1.0F);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            LOGGER.info("Waypoint click missed a block (freecam={}, reach={})", FreerCamForge.isActive(), reach);
            message("Наведитесь на блок в пределах " + (int) reach + " блоков."); return;
        }
        world = mc.world;
        BlockPos selected = hit.getBlockPos().toImmutable();
        if (route.add(selected)) {
            failedPoint = -1;
            message("Точка " + route.points().size() + " " + coordinates(selected) + " добавлена. Порядок: от игрока к цели.");
            java.util.List<BlockPos> points = route.points();
            int size = points.size();
            double x = size == 1 ? mc.player.posX : points.get(size - 2).getX() + 0.5;
            double y = size == 1 ? mc.player.getEntityBoundingBox().minY : surfaceHeight(points.get(size - 2));
            double z = size == 1 ? mc.player.posZ : points.get(size - 2).getZ() + 0.5;
            JumpPlanner.Analysis analysis = null;
            if (!(size == 1 && onSurface(selected))) {
                analysis = analyzeFrom(x, y, z, selected);
                if (analysis.plan == null) reportFailure(size, selected, analysis);
            }
            LOGGER.info("Waypoint {} added at {} from FreeCam; transition={}", size, selected,
                    analysis == null || analysis.plan != null ? "reachable" : analysis.failure);
        } else {
            LOGGER.info("Waypoint {} ignored because it is duplicate or the route is full", selected);
            message("Точка уже последняя в маршруте или достигнут лимит 32 точек.");
        }
    }

    private double surfaceHeight(BlockPos pos) {
        return surfaceHeight(pos, 0);
    }

    private double surfaceHeight(BlockPos pos, int supportDepth) {
        if (mc.world == null || !mc.world.isBlockLoaded(pos)) return Double.NaN;
        IBlockState state = mc.world.getBlockState(pos);
        if (state.getMaterial() == Material.AIR) return Double.NaN;
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        AxisAlignedBB query = new AxisAlignedBB(pos.getX() - 0.001D, pos.getY() - 0.001D, pos.getZ() - 0.001D,
                pos.getX() + 1.001D, pos.getY() + 2.501D, pos.getZ() + 1.001D);
        List<AxisAlignedBB> boxes = new ArrayList<>();
        state.addCollisionBoxToList(mc.world, pos, query, boxes, mc.player, false);
        double top = topAtCenter(boxes, centerX, centerZ, pos.getY() - 0.001D);
        if (Double.isFinite(top)) return top;
        // Collisionless/thin decorations (including a single snow layer in 1.12.2)
        // use the real supporting surface below instead of being rejected outright.
        return supportDepth < 3 && pos.getY() > 0 && mc.world.isBlockLoaded(pos.down())
                ? surfaceHeight(pos.down(), supportDepth + 1) : Double.NaN;
    }

    static double topAtCenter(List<AxisAlignedBB> boxes, double centerX, double centerZ, double minimumTop) {
        double top = Double.NaN;
        for (AxisAlignedBB box : boxes) {
            // The planned landing position is the center of the selected block. Narrow
            // supports such as fences and panes are valid if their collision reaches it.
            if (box.minX <= centerX && box.maxX >= centerX
                    && box.minZ <= centerZ && box.maxZ >= centerZ
                    && box.maxY > minimumTop) {
                top = Double.isFinite(top) ? Math.max(top, box.maxY) : box.maxY;
            }
        }
        return top;
    }

    static boolean drawableSegment(double x, double y, double z, double targetX, double targetY, double targetZ) {
        if (!Double.isFinite(x + y + z + targetX + targetY + targetZ)) return false;
        double rise = targetY - y;
        return Math.hypot(targetX - x, targetZ - z) <= 4.5D && rise <= 1.2D && rise >= -1.25D;
    }

    private boolean standable(BlockPos pos) {
        if (mc.world == null || !mc.world.isBlockLoaded(pos) || !mc.world.isBlockLoaded(pos.up(2))) return false;
        double top = surfaceHeight(pos);
        return Double.isFinite(top) && !blocked(pos.getX() + 0.5, top, pos.getZ() + 0.5);
    }

    private boolean blocked(double x, double y, double z) {
        double half = mc.player.width / 2.0;
        double height = Math.max(1.8, mc.player.height);
        AxisAlignedBB box = new AxisAlignedBB(x - half, y + 0.001, z - half, x + half, y + height, z + half);
        if (!mc.world.isBlockLoaded(new BlockPos(box.minX, box.minY, box.minZ))
                || !mc.world.isBlockLoaded(new BlockPos(box.maxX, box.maxY, box.maxZ))) return true;
        return !mc.world.getCollisionBoxes(mc.player, box).isEmpty() || mc.world.containsAnyLiquid(box);
    }

    private JumpPlanner.Analysis analyzeFrom(double x, double y, double z, BlockPos point) {
        boolean clearLanding = standable(point);
        return JumpPlanner.analyze(x, y, z, point.getX() + 0.5, surfaceHeight(point), point.getZ() + 0.5,
                (px, py, pz) -> !clearLanding || blocked(px, py, pz));
    }

    private boolean onSurface(BlockPos point) {
        return Math.floor(mc.player.posX) == point.getX() && Math.floor(mc.player.posZ) == point.getZ()
                && Math.abs(mc.player.getEntityBoundingBox().minY - surfaceHeight(point)) < 0.1;
    }

    private boolean at(BlockPos point) {
        return Math.abs(mc.player.posX - point.getX() - 0.5) < 0.35
                && Math.abs(mc.player.posZ - point.getZ() - 0.5) < 0.35
                && Math.abs(mc.player.getEntityBoundingBox().minY - surfaceHeight(point)) < 0.1;
    }

    private void startRoute() {
        if (!ready()) {
            String reason = mc.player.capabilities.isFlying ? "Выключите творческий полёт и встаньте на поверхность."
                    : !mc.player.onGround ? "Для старта нужно стоять на земле."
                    : "Прыжок недоступен: проверьте опору, воду, лестницу и эффекты прыжка/левитации.";
            message(reason); return;
        }
        if (!route.start()) { message("Маршрут пуст. P — добавить блок, в том числе из FreeCam."); return; }
        // A first point underneath the player is an anchor, not a zero-length jump.
        while (route.isRunning() && onSurface(route.current())) route.arrived();
        if (!route.isRunning()) { message("Вы уже на конечной точке."); return; }
        double x = mc.player.posX, y = mc.player.getEntityBoundingBox().minY, z = mc.player.posZ;
        int index = 0;
        for (BlockPos point : route.points()) {
            index++;
            JumpPlanner.Analysis analysis = analyzeFrom(x, y, z, point);
            if (analysis.plan == null) {
                stop(); reportFailure(index, point, analysis); return;
            }
            x = point.getX() + 0.5; y = surfaceHeight(point); z = point.getZ() + 0.5;
        }
        world = mc.world;
        controlling = true;
        failedPoint = -1;
        waitTicks = 0;
        message("Маршрут запущен: " + route.points().size() + " прыжков. O/P"
                + (FreerCamForge.isActive() ? "" : " или ручное движение") + " — остановка.");
    }

    private boolean ready() {
        return mc.world != null && mc.player != null && mc.player.onGround
                && !mc.player.isRiding() && !mc.player.isInWater() && !mc.player.isInLava()
                && !mc.player.isOnLadder() && !mc.player.capabilities.isFlying && !mc.player.isElytraFlying()
                && !mc.player.isPotionActive(MobEffects.JUMP_BOOST) && !mc.player.isPotionActive(MobEffects.LEVITATION)
                && Double.isFinite(currentSupportHeight());
    }

    private double currentSupportHeight() {
        if (mc.player == null || mc.world == null) return Double.NaN;
        double feet = mc.player.getEntityBoundingBox().minY;
        int startY = (int) Math.floor(feet + 0.001D);
        int x = (int) Math.floor(mc.player.posX);
        int z = (int) Math.floor(mc.player.posZ);
        for (int y = startY; y >= startY - 3; y--) {
            double top = surfaceHeight(new BlockPos(x, y, z));
            if (Double.isFinite(top) && Math.abs(top - feet) < 0.125D) return top;
        }
        return Double.NaN;
    }

    @SubscribeEvent
    public void input(InputUpdateEvent event) {
        if (!route.isRunning() || event.getEntityPlayer() != mc.player) return;
        // In FreeCam these keys move only the detached camera; the body remains under route control.
        if (FreerCamForge.isActive()) return;
        if (event.getMovementInput().moveForward != 0 || event.getMovementInput().moveStrafe != 0
                || event.getMovementInput().jump) { stop(); return; }
        event.getMovementInput().sneak = false;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (world != null && world != mc.world) { clear(); return; }
        if (!route.isRunning()) return;
        if (!Reference.parkour || mc.player == null || mc.player.isDead || mc.currentScreen != null
                || !mc.inGameHasFocus) { stop(); return; }
        if (plan == null) {
            if (waitTicks-- > 0) return;
            if (!ready()) { stop(); message("Маршрут остановлен: игрок не готов к следующему прыжку."); return; }
            target = route.current();
            JumpPlanner.Analysis analysis = analyzeFrom(mc.player.posX, mc.player.getEntityBoundingBox().minY, mc.player.posZ, target);
            plan = analysis.plan;
            if (plan == null) { BlockPos rejected = target; stop(); reportFailure(1, rejected, analysis); return; }
            landingY = surfaceHeight(target);
            ticks = 0;
            startX = mc.player.posX; startZ = mc.player.posZ;
            launched = false;
        }
        if (!standable(target) || Math.abs(surfaceHeight(target) - landingY) > 0.01) { stop(); message("Поверхность приземления изменилась."); return; }
        if (!launched) {
            mc.player.setSprinting(false);
            mc.player.jump();
            launched = true;
        } else if (ticks > 1 && mc.player.onGround) {
            boolean landed = at(target);
            releasePlan();
            if (!landed) { stop(); message("Маршрут остановлен: приземление вне цели."); return; }
            route.arrived();
            controlling = route.isRunning();
            waitTicks = 2;
            if (!controlling) message("Маршрут завершён.");
            return;
        } else if (Math.hypot(mc.player.posX - startX - plan.velocityX * plan.horizontalSteps(ticks),
                mc.player.posZ - startZ - plan.velocityZ * plan.horizontalSteps(ticks)) > 0.65) {
            stop(); message("Маршрут остановлен: движение отклонилось от траектории."); return;
        }
        if (++ticks > plan.ticks + 5 || (ticks > 1 && mc.player.collidedHorizontally) || mc.player.isInWater() || mc.player.isInLava()) {
            stop(); return;
        }
        double dx = target.getX() + 0.5 - mc.player.posX, dz = target.getZ() + 0.5 - mc.player.posZ;
        double distance = Math.hypot(dx, dz);
        double speed = ticks <= plan.delayTicks ? 0 : Math.min(Math.hypot(plan.velocityX, plan.velocityZ), distance);
        mc.player.motionX = distance < 0.001 ? 0 : dx / distance * speed;
        mc.player.motionZ = distance < 0.001 ? 0 : dz / distance * speed;
    }

    private void releasePlan() {
        if (plan != null && mc.player != null && mc.world == world) mc.player.motionX = mc.player.motionZ = 0;
        plan = null; target = null; launched = false;
    }

    private void stop() { releasePlan(); route.stop(); controlling = false; }
    private void clear() {
        stop();
        route.clear();
        world = null;
        failedPoint = -1;
        lastMouseSelectionTick = Integer.MIN_VALUE;
    }

    private String coordinates(BlockPos point) {
        return "[" + point.getX() + ", " + point.getY() + ", " + point.getZ() + "]";
    }

    private void reportFailure(int index, BlockPos point, JumpPlanner.Analysis analysis) {
        failedPoint = index - 1;
        String reason;
        switch (analysis.failure) {
            case TOO_FAR: reason = "слишком далеко; добавьте точки между ними"; break;
            case TOO_FAST: reason = "не хватает дальности одного прыжка на этой высоте"; break;
            case TOO_HIGH: reason = "слишком высокий подъём (предел 1.2 блока)"; break;
            case TOO_LOW: reason = "слишком большой спуск (предел 1.25 блока)"; break;
            case SAME_POSITION: reason = "точки совпадают по горизонтали"; break;
            case INVALID: reason = "поверхность не подходит или не загружена"; break;
            default: reason = "мешает блок, потолок или нет места для игрока";
        }
        message("Переход " + (index == 1 ? "игрок" : String.valueOf(index - 1)) + " → " + index + " "
                + coordinates(point) + ": " + reason + String.format(Locale.ROOT,
                " (по горизонтали %.2f, высота %+.2f).", analysis.distance, analysis.rise));
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) { if (event.getWorld() == world) clear(); }

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        if (!Reference.parkour || route.points().isEmpty() || mc.world != world || mc.getRenderViewEntity() == null) return;
        Entity view = mc.getRenderViewEntity();
        double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * event.getPartialTicks();
        double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * event.getPartialTicks();
        double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * event.getPartialTicks();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(-x, -y, -z);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glLineWidth(2);
            GL11.glColor4f(0.2F, 0.85F, 1, 1);
            GL11.glBegin(GL11.GL_LINES);
            double fromX = mc.player.posX;
            double fromY = mc.player.getEntityBoundingBox().minY;
            double fromZ = mc.player.posZ;
            for (BlockPos point : route.points()) {
                double top = surfaceHeight(point);
                double toX = point.getX() + 0.5;
                double toZ = point.getZ() + 0.5;
                if (drawableSegment(fromX, fromY, fromZ, toX, top, toZ)) {
                    GL11.glVertex3d(fromX, fromY + 0.04, fromZ);
                    GL11.glVertex3d(toX, top + 0.04, toZ);
                }
                fromX = toX;
                fromY = top;
                fromZ = toZ;
            }
            GL11.glEnd();
            boolean first = true;
            int pointIndex = 0;
            for (BlockPos point : route.points()) {
                boolean failed = pointIndex++ == failedPoint;
                GL11.glColor4f(failed || first ? 1 : 0.2F, failed ? 0.1F : first ? 0.65F : 0.85F, failed ? 0.1F : first ? 0.1F : 1, 1);
                double top = surfaceHeight(point);
                double px = point.getX(), py = Double.isFinite(top) ? top + 0.03 : point.getY() + 1.03, pz = point.getZ();
                GL11.glBegin(GL11.GL_LINE_LOOP);
                GL11.glVertex3d(px, py, pz); GL11.glVertex3d(px + 1, py, pz);
                GL11.glVertex3d(px + 1, py, pz + 1); GL11.glVertex3d(px, py, pz + 1);
                GL11.glEnd();
                first = false;
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void message(String text) { SystemOverlay.post(text); }
}
