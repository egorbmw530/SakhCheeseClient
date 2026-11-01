//
//

package com.sakhcheese.modules.shulkertooltip;

import net.minecraft.nbt.NBTTagList;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.IInventory;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;

public class ShulkerToolTip
{
    public static final String MODID = "shulkertooltip";
    private static final KeyBinding enableView;
    private static final KeyBinding openShulker;
    private static final KeyBinding toggleView;
    private static final ResourceLocation TEXTURE;

    @Mod.EventHandler
    public void preinit(final FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register((Object)new EventManager());
        ClientRegistry.registerKeyBinding(ShulkerToolTip.enableView);
        ClientRegistry.registerKeyBinding(ShulkerToolTip.openShulker);
        ClientRegistry.registerKeyBinding(ShulkerToolTip.toggleView);
    }

    static {
        enableView = new KeyBinding("key.enableView", 56, com.sakhcheese.input.KeybindsRegister.CATEGORY);
        openShulker = new KeyBinding("key.openShulker", 49, com.sakhcheese.input.KeybindsRegister.CATEGORY);
        toggleView = new KeyBinding("key.toggleView", 47, com.sakhcheese.input.KeybindsRegister.CATEGORY);
        TEXTURE = new ResourceLocation("shulkertooltip", "textures/gui/shulker_parts.png");
    }

    private class FakeShulkerBox extends GuiChest
    {
        public FakeShulkerBox(final IInventory upperInv, final IInventory lowerInv) {
            super(upperInv, lowerInv);
        }
    }

    private class EventManager extends GuiScreen
    {
        private boolean box;
        private final List<Integer> ironChestWidths;
        private float[] color;
        private boolean show;
        private boolean pressed;

        private EventManager() {
            this.box = false;
            this.ironChestWidths = Arrays.asList(2, 5, 6);
            this.show = false;
            this.pressed = false;
        }

        @SubscribeEvent
        public void tooltipEvent(final ItemTooltipEvent e) {
            if (Keyboard.isKeyDown(ShulkerToolTip.enableView.getKeyCode()) == this.show && this.box) {
                e.getToolTip().clear();
                e.getToolTip().add("");
            }
        }

        @SubscribeEvent
        public void onClick(final GuiScreenEvent.@NotNull MouseInputEvent e) {
            if (e.getGui() instanceof FakeShulkerBox) {
                e.setCanceled(true);
            }
        }

        private void boxPart(final int atX, final int atY, final int fromX, final int fromY, final int toX, final int toY) {
            GL11.glDisable(2929);
            GlStateManager.color(this.color[0] + 0.15f, this.color[1] + 0.15f, this.color[2] + 0.15f);
            Minecraft.getMinecraft().getTextureManager().bindTexture(ShulkerToolTip.TEXTURE);
            this.drawTexturedModalRect(atX, atY, fromX, fromY, toX, toY);
            GL11.glEnable(2929);
        }

        private void drawBar(final int x, final int y, final int yStart, final int boxWidth, final int howTall) {
            this.boxPart(x, y, 0, yStart, 5, howTall);
            for (int i = 0; i < boxWidth + 1; ++i) {
                this.boxPart(x + 4 + i * 18, y, 4, yStart, 18, howTall);
            }
            this.boxPart(x + 4 + (boxWidth + 1) * 18, y, 22, yStart, 6, howTall);
        }

        @SubscribeEvent
        public void event(final GuiScreenEvent.DrawScreenEvent.@NotNull Post event) {
            final EntityPlayerSP p = Minecraft.getMinecraft().player;
            if (event.getGui() instanceof GuiContainer && ((GuiContainer)event.getGui()).getSlotUnderMouse() != null) {
                final ItemStack item = ((GuiContainer)event.getGui()).getSlotUnderMouse().getStack();
                final String name = item.getItem().getRegistryName().toString().split(":")[1].replace("_", "");
                if (name.contains("shulkerbox")) {
                    if (Keyboard.isKeyDown(ShulkerToolTip.toggleView.getKeyCode())) {
                        if (!this.pressed) {
                            this.show = !this.show;
                            this.pressed = true;
                        }
                    }
                    else {
                        this.pressed = false;
                    }
                    for (final EnumDyeColor c : EnumDyeColor.values()) {
                        if (name.contains(c.toString().toLowerCase())) {
                            this.color = c.getColorComponentValues();
                            break;
                        }
                        this.color = new float[] { 1.0f, 1.0f, 1.0f };
                    }
                    final int boxWidth = this.ironChestWidths.contains(item.getMetadata()) ? 11 : 8;
                    final NBTTagList nbttaglist = (item.hasTagCompound() ? item.getTagCompound() : new NBTTagCompound()).getCompoundTag("BlockEntityTag").getTagList("Items", 10);
                    int slot = 0;
                    final List<ItemStack> boxContents = new ArrayList<ItemStack>();
                    for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                        final ItemStack stack = new ItemStack((NBTTagCompound)nbttaglist.get(i));
                        final String stringofitems = nbttaglist.get(i).toString();
                        final int slotnum = Integer.parseInt(stringofitems.substring(stringofitems.indexOf("{Slot:") + 1, stringofitems.indexOf("b,")).replaceAll("[^0-9]", ""));
                        while (slot != slotnum) {
                            ++slot;
                            boxContents.add(ItemStack.EMPTY);
                        }
                        ++slot;
                        boxContents.add(stack);
                    }
                    slot %= boxWidth + 1;
                    while (boxWidth + 1 > slot && slot != 0) {
                        ++slot;
                        boxContents.add(ItemStack.EMPTY);
                    }
                    if (Keyboard.isKeyDown(ShulkerToolTip.openShulker.getKeyCode())) {
                        if (boxWidth != 8 || boxContents.size() > 54) {
                            p.sendMessage((ITextComponent)new TextComponentString("§cERROR: Chest contents wouldn't fit in GUI."));
                        }
                        final IInventory inventory = (IInventory)new InventoryBasic(item.getDisplayName(), item.hasDisplayName(), (boxContents.size() < 27) ? 27 : boxContents.size());
                        for (int j = 0; j < boxContents.size(); ++j) {
                            inventory.setInventorySlotContents(j, (ItemStack)boxContents.get(j));
                        }
                        Minecraft.getMinecraft().displayGuiScreen((GuiScreen)new FakeShulkerBox((IInventory)p.inventory, inventory));
                    }
                    if (Keyboard.isKeyDown(ShulkerToolTip.enableView.getKeyCode()) == this.show) {
                        final int lengthOver = event.getMouseX() + 32 + boxWidth * 18 - new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
                        final int toolTipHeight = 8 + (boxContents.size() - 1) / boxWidth * 18;
                        final int centerToolTip = toolTipHeight / 2;
                        final int lengthAbove = event.getMouseY() - 23 - centerToolTip;
                        final int lengthBelow = event.getMouseY() - 10 + toolTipHeight - new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight() - centerToolTip;
                        this.box = true;
                        final int drawX = event.getMouseX() + 4 - ((lengthOver > 0) ? lengthOver : 0);
                        final int drawY = event.getMouseY() - 12 - centerToolTip - ((lengthBelow > 0) ? lengthBelow : 0) - ((lengthAbove < 0) ? lengthAbove : 0);
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(0.0f, 0.0f, 777.0f);
                        RenderHelper.enableGUIStandardItemLighting();
                        this.drawBar(drawX, drawY - 11, 0, boxWidth, 16);
                        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(item.getDisplayName(), (float)(drawX + 6), (float)(drawY - 6), 16777215);
                        int row = 0;
                        slot = 0;
                        int x = 0;
                        int y = drawY + 6 + 18 * row;
                        this.boxPart(drawX, drawY + 5, 0, 4, 5, 18);
                        this.boxPart(drawX + (boxWidth + 1) * 18 + 5, drawY + 5, 23, 5, 5, 18);
                        for (int k = 0; k < boxContents.size(); ++k) {
                            if (slot > boxWidth) {
                                slot = 0;
                                ++row;
                                this.boxPart(drawX, drawY + 5 + 18 * row, 0, 4, 5, 18);
                                this.boxPart(drawX + (boxWidth + 1) * 18 + 5, drawY + 5 + 18 * row, 23, 5, 5, 18);
                            }
                            x = drawX + 6 + 18 * slot;
                            y = drawY + 6 + 18 * row;
                            this.boxPart(x - 1, y - 1, 5, 17, 18, 18);
                            Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI((ItemStack)boxContents.get(k), x, y);
                            Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer, (ItemStack)boxContents.get(k), x, y, (boxContents.get(k).getCount() == 1) ? "" : Integer.toString(boxContents.get(k).getCount()));
                            ++slot;
                        }
                        while (boxWidth + 1 > slot) {
                            x = drawX + 6 + 18 * slot;
                            this.boxPart(x - 1, y - 1, 5, 17, 18, 18);
                            ++slot;
                        }
                        this.drawBar(drawX, drawY + (row + 1) * 18 + 5, 35, boxWidth, 5);
                        GlStateManager.popMatrix();
                    }
                }
                else {
                    this.box = false;
                }
            }
        }
    }
}
