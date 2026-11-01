package com.sakhcheese.modules.parkour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered, bounded waypoints. Completed points are consumed; stopping preserves the rest. */
public final class ParkourRoute<T> {
    public static final int MAX_POINTS = 32;
    private final List<T> points = new ArrayList<>();
    private final List<T> view = Collections.unmodifiableList(points);
    private boolean running;

    public boolean add(T point) {
        if (running || point == null || points.size() >= MAX_POINTS
                || (!points.isEmpty() && points.get(points.size() - 1).equals(point))) return false;
        points.add(point);
        return true;
    }
    public boolean undo() {
        if (running || points.isEmpty()) return false;
        points.remove(points.size() - 1);
        return true;
    }
    public boolean start() { running = !points.isEmpty(); return running; }
    public void stop() { running = false; }
    public void clear() { stop(); points.clear(); }
    public void arrived() {
        if (!running) return;
        points.remove(0);
        if (points.isEmpty()) running = false;
    }
    public T current() { return points.isEmpty() ? null : points.get(0); }
    public boolean isRunning() { return running; }
    public List<T> points() { return view; }
}
