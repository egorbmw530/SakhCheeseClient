package com.sakhcheese.core;


import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;


import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

import static net.minecraft.client.Minecraft.getMinecraft;



public class HomBot {


    public static String chest;
    private Float previousGamma;
    private String pendingAnswer;
    private long answerAt;
    private net.minecraft.world.World answerWorld;
    protected boolean isEnabled;


    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    @SideOnly(Side.CLIENT)
    public void OnChat(@NotNull ClientChatReceivedEvent event) {

        if (getMinecraft().player == null || getMinecraft().world == null) return;
        String mes = event.getMessage().getUnformattedText().trim();

        if (Reference.autoAnswer && pendingAnswer == null) {
            String expression = com.sakhcheese.util.Calk.questionExpression(mes);
            if (expression != null) {
                try {
                    String answer = com.sakhcheese.util.Calk.calculate(expression).toPlainString();
                    if (answer.length() <= 110) {
                        pendingAnswer = answer;
                        answerWorld = getMinecraft().world;
                        answerAt = System.nanoTime() + 2500000000L;
                        com.sakhcheese.hud.SystemOverlay.post("Решала: " + expression + " = " + answer + " (отправка через 2,5 с).");
                    }
                } catch (com.sakhcheese.util.Calk.InvalidDataException invalidExpression) {
                    // Ignore malformed or excessively expensive server questions.
                }
            }
        }
        boolean rybka = mes.contains("/sellfish");
        if (Reference.sellFish) {
            if (rybka){// Продажа рыбки. Можно было сделать с помощью отдельного event'a
                getMinecraft().player.sendChatMessage("/sellfish");
                System.out.println("работает же");
            }
        }



            boolean nexusC = mes.contains("/nexuschest");
            if (nexusC) {
                int nexus = mes.indexOf("/nexuschest");
                chest = mes.substring(nexus + 11);
                System.out.println(chest);
            }




    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public void onEvent(TickEvent.ClientTickEvent event){

        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = getMinecraft();
        boolean inWorld = mc.player != null && mc.world != null;
        if (Reference.fullBright && inWorld) {
            if (previousGamma == null) previousGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 15.0F;
        } else if (previousGamma != null) {
            mc.gameSettings.gammaSetting = previousGamma;
            previousGamma = null;
        }
        if (!inWorld) {
            pendingAnswer = null;
            answerWorld = null;
            chest = null;
            return;
        }
        if (!Reference.autoAnswer || mc.world != answerWorld) pendingAnswer = null;
        if (pendingAnswer != null && System.nanoTime() - answerAt >= 0) {
            mc.player.sendChatMessage(pendingAnswer);
            com.sakhcheese.hud.SystemOverlay.post("Ответ отправлен: " + pendingAnswer);
            pendingAnswer = null;
        }
        if (pendingAnswer == null) answerWorld = null;
    }
}
