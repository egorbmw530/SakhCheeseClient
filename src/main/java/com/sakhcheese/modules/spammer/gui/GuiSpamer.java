package com.sakhcheese.modules.spammer.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.config.GuiSlider;
import com.sakhcheese.modules.spammer.PropsUtil;

public class GuiSpamer extends GuiContainer {

	private GuiTextField input;
	

    
    public GuiSpamer(EntityPlayer player) {
        super(new ContainerBase(player));
        this.xSize = 120;
        this.ySize = 114;
    }
    
    @Override
	public void initGui()
	{
    	super.initGui();
		this.guiLeft = (this.width - this.xSize) / 2;
		this.guiTop = (this.height - this.ySize) / 2;
		this.buttonList.clear();
		addButtons();
        Keyboard.enableRepeatEvents(true);
	}

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		input.setMaxStringLength(110);
		input.setTextColor(0xFFFBFBFB);
    }
    
    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if(buttonList.size() > 0){
            PropsUtil.setInterval((int)((GuiSlider)this.buttonList.get(1)).getValue(), System.nanoTime());
        	this.buttonList.get(0).displayString = (PropsUtil.isActive ? "\u0412\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c" : "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c");
        }
        PropsUtil.text = input.getText();
		input.updateCursorCounter();
    }
    
	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		input.drawTextBox();
	}
    
	@Override
	protected void keyTyped(char charProp, int key)
    {
		switch(key) {
		case Keyboard.KEY_ESCAPE:
			mc.displayGuiScreen((GuiScreen)null);
			mc.setIngameFocus();
			break;
		default:
			break;
		}
        this.input.textboxKeyTyped(charProp, key);

    }
	
	@Override
	public void onGuiClosed()
    {
        PropsUtil.text = input.getText();
        PropsUtil.setInterval((int)((GuiSlider)this.buttonList.get(1)).getValue(), System.nanoTime());
        input.setFocused(false);
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        com.sakhcheese.core.ClientStateConfig.save();
    }
	
    private void addButtons()
    {
    	GuiButton activate = new GuiButton(0, guiLeft + 5, guiTop + 7, 110, 20, PropsUtil.isActive ? "\u0412\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c" : "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c");
    	GuiSlider timer = new GuiSlider(1, guiLeft + 5, guiTop + 57, 110, 20, "\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b\u003a ", " (\u0441\u0435\u043a)", PropsUtil.MIN_INTERVAL_SECONDS, 180, PropsUtil.timer, false, true);
    	GuiButton reset = new GuiButton(2, guiLeft + 5, guiTop + 82, 110, 20, "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f");
		input = new GuiTextField(3, this.fontRenderer, guiLeft + 5, guiTop + 32, 110, 20);
		input.setText(PropsUtil.text);
        input.setMaxStringLength(110);
    	activate.enabled = true;
    	timer.enabled = true;
    	reset.enabled = true;
        this.buttonList.add(activate);
        this.buttonList.add(timer);
        this.buttonList.add(reset);
    }
    

	@Override
	public void mouseClicked(int mouseX, int mouseY, int state) throws IOException {
		super.mouseClicked(mouseX, mouseY, state);
		this.input.mouseClicked(mouseX, mouseY, state);
	
	}
    
    @Override
    protected void actionPerformed(GuiButton button)
    {
		if (button.id == 0){
            PropsUtil.text = input.getText();
            long now = System.nanoTime();
            PropsUtil.setInterval((int)((GuiSlider)this.buttonList.get(1)).getValue(), now);
            PropsUtil.setActive(!PropsUtil.isActive, now);
			com.sakhcheese.hud.SystemOverlay.post("Спаммер " + (PropsUtil.isActive ? "включён (M)." : "выключен."));
				
		}
		else if (button.id == 2){
			PropsUtil.reset();
			com.sakhcheese.core.ClientStateConfig.save();
			input.setText(PropsUtil.text);
			if(buttonList.size() > 0 && this.buttonList.get(1) instanceof GuiSlider){
	        	((GuiSlider)this.buttonList.get(1)).setValue(PropsUtil.timer);
                ((GuiSlider)this.buttonList.get(1)).updateSlider();
	        }
		}
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
