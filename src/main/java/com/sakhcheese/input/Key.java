package com.sakhcheese.input;

import com.sakhcheese.gui.ModGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.sakhcheese.core.HomBot;
import com.sakhcheese.modules.freecam.FreerCamForge.*;


public class Key {
        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void onKeyPressed (InputEvent.KeyInputEvent event) {
                if(KeybindsRegister.MyKeybinding.isKeyDown()) {

                        Minecraft.getMinecraft().player.sendChatMessage("/nexuschest " + HomBot.chest );
                        System.out.println("/nexuschest " + HomBot.chest );
                }

                if(KeybindsRegister.Gui.isKeyDown()) {
                        Minecraft.getMinecraft().displayGuiScreen(new ModGui());
                }
        }


}