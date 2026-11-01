package com.sakhcheese.modules.parkour;

/** Vanilla gravity and bounded horizontal speed; checks several takeoff timings. */
public final class JumpPlanner {
    public static final double MAX_SPEED = 0.34;
    public interface CollisionSpace { boolean blocked(double x, double y, double z); }
    public enum Failure { NONE, INVALID, SAME_POSITION, TOO_FAR, TOO_HIGH, TOO_LOW, TOO_FAST, OBSTACLE }

    public static final class Plan {
        public final int ticks, delayTicks;
        public final double velocityX, velocityZ;
        Plan(int ticks, int delay, double x, double z) {
            this.ticks = ticks; delayTicks = delay; velocityX = x; velocityZ = z;
        }
        public int horizontalSteps(int completedTicks) {
            return Math.max(0, Math.min(ticks - delayTicks, completedTicks - delayTicks));
        }
    }

    public static final class Analysis {
        public final Plan plan;
        public final Failure failure;
        public final double distance, rise;
        Analysis(Plan plan, Failure failure, double distance, double rise) {
            this.plan = plan; this.failure = failure; this.distance = distance; this.rise = rise;
        }
    }

    public static Plan plan(double x, double y, double z, double targetX, double targetY,
                            double targetZ, CollisionSpace space) {
        return analyze(x, y, z, targetX, targetY, targetZ, space).plan;
    }

    public static Analysis analyze(double x, double y, double z, double targetX, double targetY,
                                   double targetZ, CollisionSpace space) {
        double dx = targetX - x, dz = targetZ - z;
        double distance = Math.hypot(dx, dz), rise = targetY - y;
        Failure invalid = null;
        if (!Double.isFinite(x + y + z + targetX + targetY + targetZ)) invalid = Failure.INVALID;
        else if (distance < 0.15) invalid = Failure.SAME_POSITION;
        else if (distance > 4.5) invalid = Failure.TOO_FAR;
        else if (rise > 1.2) invalid = Failure.TOO_HIGH;
        else if (rise < -1.25) invalid = Failure.TOO_LOW;
        if (invalid != null) return new Analysis(null, invalid, distance, rise);
        double height = y, velocityY = 0.42;
        int landing = 0;
        for (int tick = 1; tick <= 30; tick++) {
            double next = height + velocityY;
            if (velocityY < 0 && height >= targetY && next <= targetY) { landing = tick; break; }
            height = next;
            velocityY = (velocityY - 0.08) * 0.98;
        }
        if (landing == 0) return new Analysis(null, Failure.TOO_HIGH, distance, rise);
        if (distance / landing > MAX_SPEED) return new Analysis(null, Failure.TOO_FAST, distance, rise);
        for (int delay = 0; delay <= Math.min(5, landing - 1); delay++) {
            if (distance / (landing - delay) > MAX_SPEED) continue;
            Plan candidate = new Plan(landing, delay, dx / (landing - delay), dz / (landing - delay));
            if (clear(x, y, z, targetY, candidate, space)) return new Analysis(candidate, Failure.NONE, distance, rise);
        }
        return new Analysis(null, Failure.OBSTACLE, distance, rise);
    }

    private static boolean clear(double x, double y, double z, double targetY, Plan plan, CollisionSpace space) {
        double height = y, velocityY = 0.42;
        for (int tick = 1; tick <= plan.ticks; tick++) {
            double nextY = tick == plan.ticks ? targetY : height + velocityY;
            int previous = plan.horizontalSteps(tick - 1), next = plan.horizontalSteps(tick);
            for (int part = 1; part <= 4; part++) {
                double progress = part / 4.0;
                double steps = previous + (next - previous) * progress;
                if (space.blocked(x + plan.velocityX * steps, height + (nextY - height) * progress,
                        z + plan.velocityZ * steps)) return false;
            }
            height = nextY;
            velocityY = (velocityY - 0.08) * 0.98;
        }
        return true;
    }

    private JumpPlanner() { }
}
