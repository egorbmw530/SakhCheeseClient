package com.sakhcheese.modules.autosprint;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeybindHandler {

    AutoSprint asInstance;
    KeyBinding toggleKeybind;

    public KeybindHandler(AutoSprint asInstance) {
        this.asInstance = asInstance;
        toggleKeybind = new KeyBinding("keybind.toggleAutoSprint", Keyboard.KEY_R, com.sakhcheese.input.KeybindsRegister.CATEGORY);
        ClientRegistry.registerKeyBinding(toggleKeybind);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent e) {
        if(toggleKeybind.isPressed() && net.minecraft.client.Minecraft.getMinecraft().currentScreen == null
                && net.minecraft.client.Minecraft.getMinecraft().player != null) {
            asInstance.setState(!asInstance.getState());
        }
    }

}
