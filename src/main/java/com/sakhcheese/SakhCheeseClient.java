package com.sakhcheese;
import com.sakhcheese.core.*;
import com.sakhcheese.modules.hitboxes.Hitboxes;
import com.sakhcheese.modules.render.Glower;

import com.sakhcheese.modules.trajectory.ModHandler;
import com.sakhcheese.modules.autosprint.AutoSprint;
import com.sakhcheese.input.Key;
import com.sakhcheese.input.KeybindsRegister;
import com.sakhcheese.modules.freecam.FreerCamForge;
import com.sakhcheese.modules.highlighter.PlayerHighlighter;
import com.sakhcheese.modules.shulkertooltip.ShulkerToolTip;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import com.sakhcheese.modules.spammer.handlers.EventsHandler;
import com.sakhcheese.modules.spammer.handlers.KeyHandler;

@Mod(modid = SakhCheeseClient.MODID, name = SakhCheeseClient.NAME, version = SakhCheeseClient.VERSION,
        acceptedMinecraftVersions = "1.12.2", clientSideOnly = true, acceptableRemoteVersions = "*")
public class SakhCheeseClient {
    public static final String MODID = "sakhcheeseclient";
    public static final String NAME = "SakhCheeseClient";
    public static final String VERSION = "1.0.0";
    @Mod.Instance(MODID)
    public static SakhCheeseClient instance;
    private final PlayerHighlighter highlighter = new PlayerHighlighter();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ClientStateConfig.load(event.getModConfigurationDirectory());
        new AutoSprint().preInitialization(event);
        highlighter.preInit(event);
        new ShulkerToolTip().preinit(event);
        MinecraftForge.EVENT_BUS.register(new KeyHandler());
        MinecraftForge.EVENT_BUS.register(new EventsHandler());
        Hitboxes hitboxes = new Hitboxes();
        hitboxes.load(event.getModConfigurationDirectory());
        MinecraftForge.EVENT_BUS.register(hitboxes);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        KeybindsRegister.register();
        MinecraftForge.EVENT_BUS.register(new Key());
        MinecraftForge.EVENT_BUS.register(new HomBot());
        MinecraftForge.EVENT_BUS.register(new ModHandler());
        MinecraftForge.EVENT_BUS.register(new Glower());
        MinecraftForge.EVENT_BUS.register(new com.sakhcheese.modules.render.ClientBlockEffect());
        MinecraftForge.EVENT_BUS.register(new com.sakhcheese.modules.parkour.AutoParkour());
        MinecraftForge.EVENT_BUS.register(new FreerCamForge());
        MinecraftForge.EVENT_BUS.register(new com.sakhcheese.hud.SystemOverlay());
        highlighter.init(event);
        MinecraftForge.EVENT_BUS.register(new com.sakhcheese.modules.resizer.handlers.EventsHandler());
    }
}






