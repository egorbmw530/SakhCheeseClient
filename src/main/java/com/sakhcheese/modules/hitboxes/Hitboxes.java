package com.sakhcheese.modules.hitboxes;
import com.sakhcheese.core.Reference;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Expands client selection boxes between world updates, without changing vanilla collision sizes. */
public class Hitboxes {
    public static Hitboxes instance;
    public static float customWidth;
    private final Map<EntityPlayer, AxisAlignedBB[]> changed = new IdentityHashMap<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private Configuration config;

    public void load(File directory) {
        instance = this;
        config = new Configuration(new File(directory, "sakhcheeseclient-hitboxes.cfg"));
        config.load();
        Reference.BitBoxes = config.getBoolean("enabled", "hitboxes", false, "Client player selection boxes");
        customWidth = config.getFloat("extraWidth", "hitboxes", 0, 0, 4.9F, "Extra horizontal width in blocks");
        setWidth(customWidth);
        save();
    }

    public static void setEnabled(boolean enabled) {
        Reference.BitBoxes = enabled;
        if (!enabled && instance != null) instance.restore();
    }

    public static void setWidth(float width) {
        customWidth = Float.isFinite(width) ? Math.round(Math.max(0, Math.min(4.9F, width)) * 10) / 10.0F : 0;
        if (instance != null) instance.restore();
    }

    public void save() {
        config.get("hitboxes", "enabled", false).set(Reference.BitBoxes);
        config.get("hitboxes", "extraWidth", 0.0).set(customWidth);
        config.save();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) restore();
        else apply();
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        // getMouseOver runs during rendering, including frames between world ticks.
        if (event.phase == TickEvent.Phase.START) apply();
    }

    private void apply() {
        restore();
        if (!Reference.BitBoxes || customWidth <= 0 || mc.world == null || mc.player == null) return;
        for (EntityPlayer player : mc.world.playerEntities) {
            if (player == mc.player || player.isDead || player.world != mc.world) continue;
            AxisAlignedBB original = player.getEntityBoundingBox();
            AxisAlignedBB expanded = original.grow(customWidth / 2.0D, 0, customWidth / 2.0D);
            changed.put(player, new AxisAlignedBB[] {original, expanded});
            player.setEntityBoundingBox(expanded);
        }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) restore();
    }

    private void restore() {
        for (Map.Entry<EntityPlayer, AxisAlignedBB[]> entry : changed.entrySet()) {
            if (entry.getKey().getEntityBoundingBox() == entry.getValue()[1]) {
                entry.getKey().setEntityBoundingBox(entry.getValue()[0]);
            }
        }
        changed.clear();
    }
}
