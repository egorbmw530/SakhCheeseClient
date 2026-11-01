package com.sakhcheese.modules.spammer.handlers;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

public class KeyHandler
{
	private final Minecraft mc;
	
	public static final int
		SPAMER = 0;

	private static final String[] desc = {
		"key.spamer"
	};

	private static final int[] keyValues = {
		Keyboard.KEY_DELETE
	};

	public static final KeyBinding[] keys = new KeyBinding[desc.length];

	public KeyHandler() {
		mc = Minecraft.getMinecraft();
		for (int i = 0; i < desc.length; ++i) {
            if (keys[i] == null) {
                keys[i] = new KeyBinding(desc[i], keyValues[i], com.sakhcheese.input.KeybindsRegister.CATEGORY);
                ClientRegistry.registerKeyBinding(keys[i]);
            }
		}
	}

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (keys[SPAMER].isPressed() && mc.player != null && mc.world != null && mc.currentScreen == null) {
            mc.displayGuiScreen(new com.sakhcheese.modules.spammer.gui.GuiSpamer(mc.player));
        }
    }
}
