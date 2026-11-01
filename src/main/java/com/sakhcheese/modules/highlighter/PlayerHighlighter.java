package com.sakhcheese.modules.highlighter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PlayerHighlighter
{
	public static final String MODID = "playerhighlighter";
	public static final String NAME = "Player Highlighter";


	static Logger logger;

	@Mod.EventHandler
	public void preInit(@NotNull FMLPreInitializationEvent event)
	{
		logger = event.getModLog();

		ModConfig.instance.loadDefaultConfig(event);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(Renderer.instance);
	}
}
