package com.sakhcheese.modules.resizer.gui;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.config.GuiSlider;
import com.sakhcheese.modules.resizer.props.Size;


public class GuiResizer extends GuiContainer
{

    public GuiResizer(EntityPlayer player) {
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
}

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if(buttonList.size() > 0){
            Size.set((float) ((GuiSlider)this.buttonList.get(0)).getValue(),
                    (float) ((GuiSlider)this.buttonList.get(1)).getValue(),
                    (float) ((GuiSlider)this.buttonList.get(2)).getValue());
        }
    }

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);


	}
    
    private void addButtons()
    {
    	GuiSlider slider_0 = new GuiSlider(0, guiLeft + 5, guiTop + 7, 110, 20, "\u0420\u0430\u0437\u043c\u0435\u0440 \u043f\u043e \u0058\u003a ", " \u0438\u0437 1.0", 0.2D, 12.0D, Size.x, true, true);
    	GuiSlider slider_1 = new GuiSlider(1, guiLeft + 5, guiTop + 32, 110, 20, "\u0420\u0430\u0437\u043c\u0435\u0440 \u043f\u043e Y\u003a ", " \u0438\u0437 1.0", 0.2D, 12.0D, Size.y, true, true);
    	GuiSlider slider_2 = new GuiSlider(2, guiLeft + 5, guiTop + 57, 110, 20, "\u0420\u0430\u0437\u043c\u0435\u0440 \u043f\u043e Z\u003a ", " \u0438\u0437 1.0", 0.2D, 12.0D, Size.z, true, true);
    	GuiButton reset = new GuiButton(3, guiLeft + 5, guiTop + 82, 110, 20, "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f");
    	slider_0.enabled = true;
    	slider_1.enabled = true;
    	slider_2.enabled = true;
    	reset.enabled = true;
        this.buttonList.add(slider_0);
        this.buttonList.add(slider_1);
        this.buttonList.add(slider_2);
        this.buttonList.add(reset);
    }
    
    @Override
    protected void actionPerformed(@NotNull GuiButton button)
    {
		if (button.id == 3){
			Size.reset();
			((GuiSlider)this.buttonList.get(0)).setValue(1.0D);
			((GuiSlider)this.buttonList.get(1)).setValue(1.0D);
			((GuiSlider)this.buttonList.get(2)).setValue(1.0D);
			for (int i = 0; i < 3; i++) ((GuiSlider)this.buttonList.get(i)).updateSlider();
		}
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
