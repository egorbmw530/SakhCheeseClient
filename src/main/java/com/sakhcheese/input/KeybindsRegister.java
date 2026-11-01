package com.sakhcheese.input;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class KeybindsRegister {
    public static final String CATEGORY = "SakhCheeseClient";
    private static final String catergory = CATEGORY;
    public static final KeyBinding
        MyKeybinding = new KeyBinding("Nexus Chest", Keyboard.KEY_G, catergory),
            Gui = new KeyBinding("Меню", Keyboard.KEY_RSHIFT, catergory),
        FreeCamForge = new KeyBinding("Фрикам", Keyboard.KEY_F6, catergory);


    public static void register() {
        setRegister(MyKeybinding);
        setRegister(Gui);
        setRegister(FreeCamForge);

    }


    private static void setRegister(KeyBinding binding) {
        ClientRegistry.registerKeyBinding(binding);
    }

}
