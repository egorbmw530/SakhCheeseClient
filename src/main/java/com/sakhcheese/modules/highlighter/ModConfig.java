package com.sakhcheese.modules.highlighter;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;

public class ModConfig {
	static ModConfig instance = new ModConfig();
	public Configuration config = null;

	public static final String CATEGORY_LINES = "cfg.lines";
	public static final String CATEGORY_ENTITIES_PLAYERS = "cfg.entities.players";
	public static final String CATEGORY_ENTITIES_CREATURES = "cfg.entities.creatures";
	public static final String CATEGORY_ENTITIES_MONSTERS = "cfg.entities.monsters";


	public static final String[] ENTITY_CATEGORIES = {
			CATEGORY_ENTITIES_PLAYERS,
			CATEGORY_ENTITIES_CREATURES,
			CATEGORY_ENTITIES_MONSTERS,
	};

	public static final String ENTITIES_ENABLE = "cfg.entities.enable";
	public static final String ENTITIES_MAX_DISTANCE = "cfg.entities.max_distance";
	public static final String ENTITIES_MAX_VERTICAL_DISTANCE = "cfg.entities.max_vertical_distance";
	public static final String ENTITIES_COLOR_R = "cfg.entities.color.red";
	public static final String ENTITIES_COLOR_G = "cfg.entities.color.green";
	public static final String ENTITIES_COLOR_B = "cfg.entities.color.blue";

	public static final String PLAYERS_IGNORE_FAKE = "cfg.entities.players.ignore_fake";
	public static final String PLAYERS_RESPECT_TEAM_COLORS = "cfg.entities.players.respect_team_colors";
	public static final String PLAYERS_DISPLAY_MODE = "cfg.entities.players.display_mode";

	public enum PlayerDisplayMode {
		EVERYONE,
		TEAM_ONLY,
		ENEMY_ONLY,
		;

		public static String @NotNull [] getNames() {
			return Arrays.stream(PlayerDisplayMode.values()).map(Enum::toString).toArray(String[]::new);
		}

		public static @Nullable PlayerDisplayMode fromString(String string) {
			for (PlayerDisplayMode mode : PlayerDisplayMode.values()) {
				if (mode.toString().equals(string)) {
					return mode;
				}
			}

			return null;
		}
	}

	public static final String LINES_ENABLE = "cfg.lines.enable";
	public static final String LINES_SMOOTH = "cfg.lines.smooth";
	public static final String LINES_INFINITE = "cfg.lines.infinite";
	public static final String LINES_WIDTH = "cfg.lines.width";
	public static final String LINES_CAMERA_DISTANCE = "cfg.lines.camera_distance";

	void loadDefaultConfig(@NotNull FMLPreInitializationEvent event) {
		File configFile = new File(event.getModConfigurationDirectory(), "playerhighlighter.cfg");

		config = new Configuration(configFile);

		Renderer.drawLines = config.get(CATEGORY_LINES, LINES_ENABLE, true).setRequiresMcRestart(true).getBoolean();
		config.get(CATEGORY_LINES, LINES_SMOOTH, true);
		config.get(CATEGORY_LINES, LINES_INFINITE, true);
		config.get(CATEGORY_LINES, LINES_WIDTH, 1.4).setMinValue(0.1).setMaxValue(10.0).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
		config.get(CATEGORY_LINES, LINES_CAMERA_DISTANCE, 1.0).setMinValue(-10.0).setMaxValue(30.0).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);

		for (String category : ENTITY_CATEGORIES) {
			config.get(category, ENTITIES_ENABLE, true);
			config.get(category, ENTITIES_MAX_DISTANCE, 64).setMinValue(1).setMaxValue(1024).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
			config.get(category, ENTITIES_MAX_VERTICAL_DISTANCE, category.equals(CATEGORY_ENTITIES_PLAYERS) ? 32 : 12).setMinValue(1).setMaxValue(512).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
			config.get(category, ENTITIES_COLOR_R, category.equals(CATEGORY_ENTITIES_PLAYERS) ? 255 : 0).setMinValue(0).setMaxValue(255).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
			config.get(category, ENTITIES_COLOR_G, category.equals(CATEGORY_ENTITIES_CREATURES) ? 255 : 0).setMinValue(0).setMaxValue(255).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
			config.get(category, ENTITIES_COLOR_B, category.equals(CATEGORY_ENTITIES_MONSTERS) ? 255 : 0).setMinValue(0).setMaxValue(255).setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
		}

		config.get(CATEGORY_ENTITIES_PLAYERS, PLAYERS_IGNORE_FAKE, true);
		config.get(CATEGORY_ENTITIES_PLAYERS, PLAYERS_RESPECT_TEAM_COLORS, true);
		config.get(CATEGORY_ENTITIES_PLAYERS, PLAYERS_DISPLAY_MODE, PlayerDisplayMode.EVERYONE.toString()).setValidValues(PlayerDisplayMode.getNames());

		// migrate old config keys
		getPlayerConfig().remove("cfg.entities.players.ignore_own_team");
		if (config.hasKey(CATEGORY_ENTITIES_PLAYERS, "cfg.entities.players.ignore_teammates")) {
			Property ignoreTeammates = getPlayerConfig().get("cfg.entities.players.ignore_teammates");
			getPlayerConfig().get(PLAYERS_DISPLAY_MODE).set(ignoreTeammates.getBoolean() ? PlayerDisplayMode.ENEMY_ONLY.toString() : PlayerDisplayMode.EVERYONE.toString());
			getPlayerConfig().remove(ignoreTeammates.getName());
		}

		config.save();
	}

	/**
	 * Convenience method to get player config quickly
	 *
	 * @return ConfigCategory
	 */
	public static ConfigCategory getPlayerConfig()
	{
		return instance.config.getCategory(ModConfig.CATEGORY_ENTITIES_PLAYERS);
	}
}
