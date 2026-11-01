package com.sakhcheese.modules.highlighter;

import com.sakhcheese.core.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.ConfigCategory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

class EntityHelper {
	static @Nullable String getEntityCategory(Entity entity)
	{
		if(Reference.tracers)
		{
		if (entity instanceof EntityOtherPlayerMP) {
			if (isFakePlayer((EntityOtherPlayerMP) entity)) {
				return null;
			}

			Team team = entity.getTeam();
			if (team != null) {
				ModConfig.PlayerDisplayMode displayMode = ModConfig.PlayerDisplayMode.fromString(ModConfig.getPlayerConfig().get(ModConfig.PLAYERS_DISPLAY_MODE).getString());
				if (displayMode == null) {
					PlayerHighlighter.logger.warn("Getting displayMode from config should have never returned null. What happened?");
				} else {
					boolean sameTeam = team.equals(Minecraft.getMinecraft().player.getTeam());

					switch (displayMode) {
						case TEAM_ONLY:
							if (!sameTeam)
								return null;
							break;
						case ENEMY_ONLY:
							if (sameTeam)
								return null;
							break;
					}
				}
			}

			return ModConfig.CATEGORY_ENTITIES_PLAYERS;
			}
		}

		// other players - like *the* player - should be ignored
		if (entity instanceof EntityPlayer)
		{
			return null;
		}

		if (entity instanceof EntityLiving)
		{
			if(Reference.tracers)
			{
				EntityLiving living = (EntityLiving) entity;

				// nobody cares about ambient creatures - hopefully
				if (entity instanceof EntityAmbientCreature)
				{
					return null;
				}

				if (living.isCreatureType(EnumCreatureType.MONSTER, false))
				{
					return ModConfig.CATEGORY_ENTITIES_MONSTERS;
				}

				if (living.isCreatureType(EnumCreatureType.CREATURE, false) || living.isCreatureType(EnumCreatureType.WATER_CREATURE, false))
				{
					return ModConfig.CATEGORY_ENTITIES_CREATURES;
				}

			}
		}

		return null;
	}

	private static boolean isFakePlayer(EntityOtherPlayerMP entity)
	{
		if(Reference.tracers) {
			// do we even want to check?
			if (!ModConfig.getPlayerConfig().get(ModConfig.PLAYERS_IGNORE_FAKE).getBoolean()) {
				return false;
			}

			UUID id = entity.getGameProfile().getId();
			if (id == null) {
				// definitely fake
				return true;
			}

			NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
			// we don't have connection but have "other player", therefore it *must be* fake
			// this is a weird edge case that shouldn't really happen or matter
			if (connection == null) {
				return true;
			}

			for (NetworkPlayerInfo playerInfo : connection.getPlayerInfoMap()) {
				if (id.equals(playerInfo.getGameProfile().getId())) {
					return false;
				}
			}
		}
			return true;

	}

	@Contract("_, _ -> new")
	static @NotNull RGBColor getRenderColor(ConfigCategory configCategory, Entity entity)
	{
		if(Reference.tracers)
		{
				Team team = entity.getTeam();
				if (team != null && ModConfig.getPlayerConfig().get(ModConfig.PLAYERS_RESPECT_TEAM_COLORS).getBoolean())
				{
					TextFormatting teamColor = team.getColor();
					//noinspection ConstantConditions
					if (teamColor != null && teamColor.isColor())
					{
						// because someone is retarded and doesn't have a getter for the char, we need to extract it from the formatting string
						int colorCode = Minecraft.getMinecraft().fontRenderer.getColorCode(teamColor.toString().charAt(1));

						// copied from FontRenderer - again because for whatever reason there's no public method to convert the color
						return new RGBColor((float) (colorCode >> 16) / 255.0F, (float) (colorCode >> 8 & 255) / 255.0F, (float) (colorCode & 255) / 255.0F);
					}
				}
		}

		float red = configCategory.get(ModConfig.ENTITIES_COLOR_R).getInt() / 255F;
		float green = configCategory.get(ModConfig.ENTITIES_COLOR_G).getInt() / 255F;
		float blue = configCategory.get(ModConfig.ENTITIES_COLOR_B).getInt() / 255F;

		return new RGBColor(red, green, blue);
	}
}
