package com.sakhcheese.modules.spammer;

public class PropsUtil {
    public static final int MIN_INTERVAL_SECONDS = 4;
    public static final int MAX_INTERVAL_SECONDS = 180;
    public static volatile boolean isActive = false;
    public static String text = "\u0422\u0435\u043a\u0441\u0442\u002c \u043a\u043e\u0442\u043e\u0440\u044b\u0439 \u044f \u0433\u043e\u0432\u043e\u0440\u044e";
    public static int timer = MIN_INTERVAL_SECONDS;
    private static long nextSend;
    private static boolean scheduled;

    public static void setInterval(int seconds, long now) {
        int interval = Math.max(MIN_INTERVAL_SECONDS, Math.min(MAX_INTERVAL_SECONDS, seconds));
        if (timer != interval) {
            timer = interval;
            scheduled = false;
            if (isActive) schedule(now);
        }
    }

    public static void setActive(boolean active, long now) {
        stop();
        isActive = active && text != null && !text.trim().isEmpty();
        if (isActive) schedule(now);
    }

    public static void stop() {
        isActive = false;
        scheduled = false;
    }

    private static void schedule(long now) {
        timer = Math.max(MIN_INTERVAL_SECONDS, Math.min(MAX_INTERVAL_SECONDS, timer));
        nextSend = now + timer * 1_000_000_000L;
        scheduled = true;
    }

    /** At most one message per poll. Missed intervals are discarded. Client thread only. */
    public static String pollMessage(long now) {
        if (!isActive) {
            scheduled = false;
            return null;
        }
        if (text == null || text.trim().isEmpty()) {
            stop();
            return null;
        }
        if (!scheduled) schedule(now);
        if (now - nextSend < 0) return null;
        schedule(now);
        return text.length() > 110 ? text.substring(0, 110) : text;
    }

    public static void reset() {
        stop();
        text = "";
        timer = MIN_INTERVAL_SECONDS;
    }
}
