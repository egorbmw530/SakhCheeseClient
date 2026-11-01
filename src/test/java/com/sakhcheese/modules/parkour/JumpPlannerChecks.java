package com.sakhcheese.modules.parkour;

import java.util.Arrays;
import java.util.Collections;
import net.minecraft.util.math.AxisAlignedBB;

public final class JumpPlannerChecks {
    private static int checks;
    public static void main(String[] args) {
        JumpPlanner.Plan level = JumpPlanner.plan(0.5, 1, 0.5, 3.5, 1, 0.5, (x, y, z) -> false);
        check(level != null, "level three-block jump");
        check(Math.abs(0.5 + level.horizontalSteps(level.ticks) * level.velocityX - 3.5) < 0.00001, "lands at target center");
        check(JumpPlanner.plan(0, 1, 0, 3, 1, 0, (x, y, z) -> x >= 1 && x <= 2) == null, "wall blocks path");
        check(JumpPlanner.plan(0, 1, 0, 2, 1, 0, (x, y, z) -> y > 1.2) == null, "low ceiling");
        check(JumpPlanner.plan(0, 1, 0, 6, 1, 0, (x, y, z) -> false) == null, "too far");
        check(JumpPlanner.plan(0, 1, 0, 2, 3, 0, (x, y, z) -> false) == null, "too high");
        check(JumpPlanner.plan(0, 3, 0, 2, 1, 0, (x, y, z) -> false) == null, "large drop");
        check(JumpPlanner.plan(0, 1, 0, 2, 2, 0, (x, y, z) -> false) != null, "one block up");
        check(JumpPlanner.plan(0, 2, 0, 2, 1, 0, (x, y, z) -> false) != null, "one block down");
        check(JumpPlanner.plan(0, 1, 0, 0, 1, 0, (x, y, z) -> false) == null, "same location");
        check(JumpPlanner.plan(0, 1, 0, Double.NaN, 1, 0, (x, y, z) -> false) == null, "invalid coordinates");
        check(JumpPlanner.plan(0, 1, 0, 2, 1, 2, (x, y, z) -> false) != null, "diagonal");
        check(JumpPlanner.plan(0, 1, 0, 4.4, 1, 0, (x, y, z) -> false) == null, "speed limit");
        // Full-block course: start block x=0, landing block x=3, gap between them.
        check(JumpPlanner.plan(0.5, 1, 0.5, 3.5, 1, 0.5,
                (x, y, z) -> y < 1 - 0.0001 && (x - 0.3 < 1 || x + 0.3 > 3)) != null,
                "support blocks do not obstruct clear jump");
        check(JumpPlanner.plan(0.5, 1, 0.5, 1.5, 2, 0.5,
                (x, y, z) -> intersects(x, y, z, 0, 0, 0, 1, 1, 1)
                        || intersects(x, y, z, 1, 1, 0, 2, 2, 1)) != null,
                "adjacent raised block is reachable after ascending before moving forward");
        JumpPlanner.Plan raised = JumpPlanner.plan(0.5, 1, 0.5, 1.5, 2, 0.5,
                (x, y, z) -> intersects(x, y, z, 0, 0, 0, 1, 1, 1)
                        || intersects(x, y, z, 1, 1, 0, 2, 2, 1));
        check(raised.delayTicks > 0 && raised.horizontalSteps(raised.delayTicks) == 0, "ascend before moving horizontally");
        check(Math.abs(raised.horizontalSteps(raised.ticks) * raised.velocityX - 1) < 0.00001, "delayed jump reaches target");
        check(Math.hypot(raised.velocityX, raised.velocityZ) <= JumpPlanner.MAX_SPEED, "delayed jump respects speed limit");
        check(JumpPlanner.plan(0.5, 1.125, 0.5, 1.5, 2.125, 0.5,
                (x, y, z) -> intersects(x, y, z, 0, 0, 0, 1, 1.125, 1)
                        || intersects(x, y, z, 1, 1, 0, 2, 2.125, 1)) != null, "raised snowy surfaces");
        check(JumpPlanner.plan(0.5, 1, 0.5, 1.5, 0.5, 0.5,
                (x, y, z) -> intersects(x, y, z, 0, 0, 0, 1, 1, 1)
                        || intersects(x, y, z, 1, 0, 0, 2, 0.5, 1)) != null, "landing on lower slab");
        check(JumpPlanner.analyze(0, 1, 0, 6, 1, 0, (x, y, z) -> false).failure == JumpPlanner.Failure.TOO_FAR, "distance diagnostic");
        check(JumpPlanner.analyze(0, 1, 0, 1, 3, 0, (x, y, z) -> false).failure == JumpPlanner.Failure.TOO_HIGH, "height diagnostic");
        check(JumpPlanner.analyze(0, 3, 0, 1, 1, 0, (x, y, z) -> false).failure == JumpPlanner.Failure.TOO_LOW, "drop diagnostic");
        check(JumpPlanner.analyze(0, 1, 0, 3, 1, 0, (x, y, z) -> true).failure == JumpPlanner.Failure.OBSTACLE, "obstruction diagnostic");
        check(AutoParkour.topAtCenter(Collections.singletonList(new AxisAlignedBB(0, 0, 0, 1, 1, 1)), 0.5, 0.5, -0.001) == 1,
                "full block support height");
        check(AutoParkour.topAtCenter(Collections.singletonList(new AxisAlignedBB(0.375, 0, 0.375, 0.625, 1.5, 0.625)), 0.5, 0.5, -0.001) == 1.5,
                "narrow fence support at block center");
        check(Double.isNaN(AutoParkour.topAtCenter(Collections.singletonList(new AxisAlignedBB(0, 0, 0, 0.1875, 1, 1)), 0.5, 0.5, -0.001)),
                "vertical open trapdoor falls back to support below");
        check(AutoParkour.topAtCenter(Arrays.asList(new AxisAlignedBB(0, 0, 0, 1, 0.5, 1),
                new AxisAlignedBB(0, 0.5, 0, 1, 1, 0.5)), 0.5, 0.5, -0.001) == 1,
                "highest stair collision at center");
        check(JumpPlanner.plan(0.5, 1, 0.5, 1.5, 1.5, 0.5,
                (x, y, z) -> intersects(x, y, z, 0, 0, 0, 1, 1, 1)
                        || intersects(x, y, z, 1.375, 0, 0.375, 1.625, 1.5, 0.625)) != null,
                "jump from full block onto fence top");
        check(JumpPlanner.plan(0.5, 1.5, 0.5, 1.5, 1, 0.5,
                (x, y, z) -> intersects(x, y, z, 0.375, 0, 0.375, 0.625, 1.5, 0.625)
                        || intersects(x, y, z, 1, 0, 0, 2, 1, 1)) != null,
                "jump from fence top onto full block");
        check(AutoParkour.drawableSegment(0.5, 1, 0.5, 4.5, 1, 0.5),
                "reachable route segment is drawn");
        check(!AutoParkour.drawableSegment(0.5, 1, 0.5, 66.5, 1, 0.5),
                "distant saved waypoint has no misleading line");
        System.out.println("AutoParkour planner: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }

    private static boolean intersects(double x, double y, double z,
                                      double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return x + 0.3 > minX && x - 0.3 < maxX && y + 1.8 > minY && y + 0.001 < maxY
                && z + 0.3 > minZ && z - 0.3 < maxZ;
    }
}
