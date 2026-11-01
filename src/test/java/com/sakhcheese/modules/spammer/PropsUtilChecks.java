package com.sakhcheese.modules.spammer;

/** Deterministic scheduler checks, no server connection or third-party test library. */
public final class PropsUtilChecks {
    private static final long SECOND = 1_000_000_000L;

    public static void main(String[] args) {
        PropsUtil.reset();
        PropsUtil.text = "test";
        check(PropsUtil.pollMessage(10 * SECOND) == null, "must require explicit start");
        PropsUtil.setInterval(1, 0);
        check(PropsUtil.timer == 4, "rate limit");
        PropsUtil.setActive(true, 0);
        check(PropsUtil.pollMessage(4 * SECOND - 1) == null, "no early send");
        check("test".equals(PropsUtil.pollMessage(4 * SECOND)), "first deadline");
        check(PropsUtil.pollMessage(4 * SECOND) == null, "duplicate poll");
        check("test".equals(PropsUtil.pollMessage(100 * SECOND)), "one send after a stall");
        check(PropsUtil.pollMessage(100 * SECOND + 1) == null, "no catch-up queue");
        PropsUtil.stop();
        check(PropsUtil.pollMessage(200 * SECOND) == null, "immediate stop");
        PropsUtil.setActive(true, 200 * SECOND);
        PropsUtil.setInterval(10, 201 * SECOND);
        check(PropsUtil.pollMessage(210 * SECOND) == null, "interval change restarts deadline");
        check("test".equals(PropsUtil.pollMessage(211 * SECOND)), "new interval");
        PropsUtil.text = " ";
        check(PropsUtil.pollMessage(300 * SECOND) == null && !PropsUtil.isActive, "blank text stops");
        PropsUtil.text = null;
        PropsUtil.setActive(true, 0);
        check(!PropsUtil.isActive, "null text cannot start");
        PropsUtil.text = "test";
        PropsUtil.setInterval(Integer.MAX_VALUE, 0);
        check(PropsUtil.timer == 180, "upper interval bound");
        // nanoTime may be negative or wrap; only elapsed differences are meaningful.
        PropsUtil.setInterval(4, 0);
        long nearWrap = Long.MAX_VALUE - SECOND;
        PropsUtil.setActive(true, nearWrap);
        check(PropsUtil.pollMessage(nearWrap + 3 * SECOND) == null, "wrap: before deadline");
        check("test".equals(PropsUtil.pollMessage(nearWrap + 4 * SECOND)), "wrap: deadline");
        PropsUtil.reset();
        System.out.println("Spammer scheduler: 15 checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
