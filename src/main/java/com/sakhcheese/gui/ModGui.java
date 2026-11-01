package com.sakhcheese.gui;

import com.sakhcheese.core.Reference;
import com.sakhcheese.core.ClientStateConfig;
import com.sakhcheese.hud.SystemOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import java.io.IOException;

public class ModGui extends GuiScreen
{
    private MenuLayout layout;
    private GuiButton mButtonClose;

    private GuiButton autoAnswerButton;
    private GuiButton sellFishButton;
    private GuiButton BitBoxesButton;
    private GuiButton fullBrightButton;
    private GuiButton tracersButton;
    private GuiButton trajectoryButton;
    private GuiButton putInButton;
    private  GuiButton parkourButton;
    private GuiButton zamorozkaButton;
    private GuiButton glowerButton;
    private GuiButton spammerbutton;

    private String on = "\u00A72On";
    private String off = "\u00A74Off";

    @Override
    public void initGui()
    {
        super.initGui();
        this.buttonList.clear();
        this.labelList.clear();
        layout = new MenuLayout(this.width, this.height, 420, 300);
        this.width = layout.width;
        this.height = layout.height;
        this.buttonList.add(mButtonClose = new GuiButton(0, this.width / 2 - 100, this.height - (this.height / 4) - 0, "\u00A74\u0417\u0430\u043a\u0440\u044b\u0442\u044c "));

        // Сменяемая кнопка
        if (Reference.autoAnswer)
        {
            this.buttonList.add(autoAnswerButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 90, "\u0420\u0435\u0448\u0430\u043b\u0430 " + on));
        }
        else
        {
            this.buttonList.add(autoAnswerButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 90, "\u0420\u0435\u0448\u0430\u043b\u0430 " + off));
        }

        if (Reference.zamorozka)
        {
            this.buttonList.add(zamorozkaButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 165, "\u043e\u0431\u043e\u0441\u0440\u044b\u0448\u044c " + on));
        }
        else
        {
            this.buttonList.add(zamorozkaButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 165, "\u043e\u0431\u043e\u0441\u0440\u044b\u0448\u044c " + off));
        }
        if (Reference.glower)
        {
            this.buttonList.add(glowerButton = new GuiButton(3, this.width / 2 - 0, this.height - (this.height / 4) - 165, "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 " + on));
        }
        else
        {
            this.buttonList.add(glowerButton = new GuiButton(3, this.width / 2 - 0, this.height - (this.height / 4) - 165, "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 " + off));
        }

        if (Reference.sellFish)
        {
            this.buttonList.add(sellFishButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 65, "\u041f\u0440\u043e\u0434\u0430\u0436\u0430 \u0440\u044b\u043f\u043a\u0438 " + on));
        }
        else
        {
            this.buttonList.add(sellFishButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 65, "\u041f\u0440\u043e\u0434\u0430\u0436\u0430 \u0440\u044b\u043f\u043a\u0438 " + off));
        }
        if (Reference.BitBoxes)
        {
            this.buttonList.add(BitBoxesButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 115, "Хитбоксы " + on));
        }
        else
        {
            this.buttonList.add(BitBoxesButton = new GuiButton(1, this.width / 2 - 200, this.height - (this.height / 4) - 115, "Хитбоксы " + off));
        }
        if (Reference.fullBright)
        {
            this.buttonList.add(fullBrightButton = new GuiButton(3, this.width /2 - 200, this.height - (this.height / 4) - 140, "FullBright " + on));
        }
        else
        {
            this.buttonList.add(fullBrightButton = new GuiButton(3, this.width /2 - 200, this.height - (this.height / 4) - 140, "FullBright " + off));
        }
        if (Reference.tracers)
        {
            this.buttonList.add(tracersButton = new GuiButton(3, this.width /2 - 0, this.height - (this.height / 4) - 140, "\u0422\u0440\u0435\u0439\u0441\u0435\u0440\u0430 " + on));
        }
        else
        {
            this.buttonList.add(tracersButton = new GuiButton(3, this.width /2 - 0, this.height - (this.height / 4) - 140, "\u0422\u0440\u0435\u0439\u0441\u0435\u0440\u0430 " + off));
        }
        if (Reference.trajectory)
        {
            this.buttonList.add(trajectoryButton = new GuiButton(3, this.width /2 - 0, this.height - (this.height / 4) - 115, "\u0422\u0440\u0430\u0435\u043a\u0442\u043e\u0440\u0438\u0438 " + on));
        }
        else
        {
            this.buttonList.add(trajectoryButton = new GuiButton(3, this.width /2 - 0, this.height - (this.height / 4) - 115, "\u0422\u0440\u0430\u0435\u043a\u0442\u043e\u0440\u0438\u0438 " + off));
        }

        this.buttonList.add(putInButton = new GuiButton(3, this.width / 2 - 0, this.height - (this.height / 4) - 90, "\u041f\u0440\u0438\u043a\u043e\u043b \u0444\u0443\u043d\u043a\u0446\u0438\u044f "));

        if (Reference.parkour)
        {
            this.buttonList.add(parkourButton = new GuiButton(3, this.width /2 - 0, this.height - (this.height / 4) - 65, "\u041f\u0430\u0440\u043a\u0443\u0440 \u002d \u043d\u043e\u0432\u043e\u0435 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435 " + on));
        }
        else
        {
            this.buttonList.add(parkourButton = new GuiButton(3, this.width /2 - 0, this.height - (this.height / 4) - 65, "\u041f\u0430\u0440\u043a\u0443\u0440 \u002d \u043d\u043e\u0432\u043e\u0435 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435 " + off));
        }
        this.buttonList.add(spammerbutton = new GuiButton(3, this.width / 2 - 0, this.height - (this.height / 4) - 40, "spammer"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button == mButtonClose)
        {
            mc.player.closeScreen();
        }

        else if (button == autoAnswerButton)
        {
            if (!Reference.autoAnswer)
            {
                Reference.autoAnswer = true;
                button.displayString = "\u0420\u0435\u0448\u0430\u043b\u0430 " + on;
            } else
            {
                Reference.autoAnswer = false;
                button.displayString = "\u0420\u0435\u0448\u0430\u043b\u0430 " + off;
            }
            featureChanged("Решала", Reference.autoAnswer);
        }

        else if (button == sellFishButton)
        {
            if (!Reference.sellFish)
            {
                Reference.sellFish = true;
                button.displayString = "\u041f\u0440\u043e\u0434\u0430\u0436\u0430 \u0440\u044b\u043f\u043a\u0438" + on;
            } else
            {
                Reference.sellFish = false;
                button.displayString = "\u041f\u0440\u043e\u0434\u0430\u0436\u0430 \u0440\u044b\u043f\u043a\u0438" + off;
            }
            featureChanged("Продажа рыбки", Reference.sellFish);
        }
        else if (button == BitBoxesButton)
        {
            mc.displayGuiScreen(new GuiHitboxes(this));
        }

        else if (button == fullBrightButton)
        {
            if (!Reference.fullBright)
            {
                Reference.fullBright =  true;
                button.displayString = "FullBright " + on;
            } else
            {
                Reference.fullBright = false;
                button.displayString = "FullBright " + off;
            }
            featureChanged("FullBright", Reference.fullBright);
        }
        else if (button == tracersButton)
        {
            if (!Reference.tracers)
            {
                Reference.tracers =  true;
                button.displayString = "\u0422\u0440\u0435\u0439\u0441\u0435\u0440\u0430 " + on;
            } else
            {
                Reference.tracers =  false;
                button.displayString = "\u0422\u0440\u0435\u0439\u0441\u0435\u0440\u0430 " + off;
            }
            featureChanged("Трейсеры", Reference.tracers);
        }
        else if (button == trajectoryButton)
        {
            if (!Reference.trajectory)
            {
                Reference.trajectory =  true;
                button.displayString = "\u0422\u0440\u0430\u0435\u043a\u0442\u043e\u0440\u0438\u0438 " + on;
            } else
            {
                Reference.trajectory =  false;
                button.displayString = "\u0422\u0440\u0430\u0435\u043a\u0442\u043e\u0440\u0438\u0438 " + off;
            }
            featureChanged("Траектории", Reference.trajectory);
        }
        else if (button == parkourButton)
        {
            if (!Reference.parkour)
            {
                Reference.parkour =  true;
                button.displayString = "\u041f\u0430\u0440\u043a\u0443\u0440 \u002d \u043d\u043e\u0432\u043e\u0435 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435 " + on;
                SystemOverlay.post("Автопаркур: P или ЛКМ в FreeCam — точка, O — запуск/стоп, Backspace — удалить.");
            } else
            {
                Reference.parkour =  false;
                button.displayString = "\u041f\u0430\u0440\u043a\u0443\u0440 \u002d \u043d\u043e\u0432\u043e\u0435 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435 " + off;
            }
            featureChanged("Автопаркур", Reference.parkour);
        }
        else if (button == zamorozkaButton)
        {
            if (!Reference.zamorozka)
            {
                Reference.zamorozka =  true;
                button.displayString = "\u043e\u0431\u043e\u0441\u0440\u044b\u0448\u044c " + on;

            } else
            {
                Reference.zamorozka =  false;
                button.displayString = "\u043e\u0431\u043e\u0441\u0440\u044b\u0448\u044c " + off;
            }
            featureChanged("Обосрыш", Reference.zamorozka);
        }
        else if (button == glowerButton)
        {
            if (!Reference.glower)
            {
                Reference.glower = true;
                button.displayString = "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 " + on;
            } else
            {
                Reference.glower = false;
                button.displayString = "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 " + off;
            }
            featureChanged("Подсветка", Reference.glower);
        }

        if (button == putInButton)
        {
            if (mc.player != null && mc.world != null) mc.displayGuiScreen(new com.sakhcheese.modules.resizer.gui.GuiResizer(mc.player));
        }
        if(button == spammerbutton)
        {
            if (mc.player != null && mc.world != null) mc.displayGuiScreen(new com.sakhcheese.modules.spammer.gui.GuiSpamer(mc.player));
        }


    }

    private void featureChanged(String name, boolean enabled) {
        ClientStateConfig.save();
        SystemOverlay.post(name + (enabled ? " включена." : " выключена."));
    }




    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        try {
            net.minecraft.client.renderer.GlStateManager.scale(layout.scaleX, layout.scaleY, 1);
            this.drawDefaultBackground();
            super.drawScreen(layout.mouseX(mouseX), layout.mouseY(mouseY), partialTicks);
        } finally {
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

}
