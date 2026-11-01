package com.sakhcheese.core;

import java.io.File;
import com.sakhcheese.modules.spammer.PropsUtil;
import net.minecraftforge.common.config.Configuration;

/** Persists user-selected client features. Session-only actions are kept out of this file. */
public final class ClientStateConfig {
    private static Configuration config;

    private ClientStateConfig() { }

    public static void load(File directory) {
        config = new Configuration(new File(directory, "sakhcheeseclient.cfg"));
        config.load();
        Reference.autoAnswer = get("autoAnswer", false);
        Reference.sellFish = get("sellFish", false);
        Reference.fullBright = get("fullBright", false);
        Reference.tracers = get("tracers", false);
        Reference.trajectory = get("trajectory", false);
        Reference.parkour = get("parkour", false);
        Reference.zamorozka = get("blockEffect", false);
        Reference.glower = get("glower", false);
        PropsUtil.text = config.getString("text", "spammer", PropsUtil.text, "Last entered text");
        PropsUtil.setInterval(config.getInt("interval", "spammer", PropsUtil.MIN_INTERVAL_SECONDS,
                PropsUtil.MIN_INTERVAL_SECONDS, PropsUtil.MAX_INTERVAL_SECONDS, "Interval in seconds"), System.nanoTime());
        save();
    }

    private static boolean get(String name, boolean fallback) {
        return config.getBoolean(name, "features", fallback, "Last user-selected state");
    }

    public static void save() {
        if (config == null) return;
        set("autoAnswer", Reference.autoAnswer);
        set("sellFish", Reference.sellFish);
        set("fullBright", Reference.fullBright);
        set("tracers", Reference.tracers);
        set("trajectory", Reference.trajectory);
        set("parkour", Reference.parkour);
        set("blockEffect", Reference.zamorozka);
        set("glower", Reference.glower);
        config.get("spammer", "text", "").set(PropsUtil.text == null ? "" : PropsUtil.text);
        config.get("spammer", "interval", PropsUtil.MIN_INTERVAL_SECONDS).set(PropsUtil.timer);
        config.save();
    }

    private static void set(String name, boolean value) {
        config.get("features", name, false).set(value);
    }
}
