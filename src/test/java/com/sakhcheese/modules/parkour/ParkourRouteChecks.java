package com.sakhcheese.modules.parkour;

public final class ParkourRouteChecks {
    private static int checks;
    public static void main(String[] args) {
        ParkourRoute<Integer> route = new ParkourRoute<>();
        check(!route.start(), "cannot start an empty route");
        check(!route.add(null), "no null points");
        check(route.add(1) && route.add(2) && route.add(3), "ordered selection");
        check(!route.add(3), "no consecutive duplicate");
        check(route.undo() && route.points().size() == 2, "undo last point");
        check(route.start() && route.current() == 1, "explicit start");
        check(!route.add(4) && !route.undo(), "editing blocked while running");
        route.arrived();
        check(route.current() == 2 && route.isRunning(), "advance after landing");
        route.stop();
        check(!route.isRunning() && route.current() == 2, "stop preserves remaining points");
        route.arrived();
        check(route.current() == 2, "idle landing does not consume a point");
        route.start();
        route.arrived();
        check(!route.isRunning() && route.current() == null, "finish does not loop");
        for (int i = 0; i < ParkourRoute.MAX_POINTS; i++) check(route.add(i), "bounded route accepts point");
        check(!route.add(999), "bounded route rejects overflow");
        try { route.points().clear(); throw new AssertionError("Mutable route view"); }
        catch (UnsupportedOperationException expected) { checks++; }
        route.start();
        route.clear();
        check(!route.isRunning() && route.points().isEmpty(), "world change clears route");
        System.out.println("Parkour route: " + checks + " checks passed");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
