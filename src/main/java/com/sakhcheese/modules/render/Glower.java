package com.sakhcheese.modules.render;
import com.sakhcheese.core.Reference;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Glower {
    private final Map<Entity, Boolean> changed = new IdentityHashMap<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!Reference.glower || mc.world == null || mc.player == null) {
            restore();
            return;
        }
        changed.entrySet().removeIf(entry -> {
            Entity entity = entry.getKey();
            if (entity.world == mc.world && !entity.isDead && mc.world.loadedEntityList.contains(entity)) return false;
            entity.setGlowing(entry.getValue());
            return true;
        });
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity == mc.player || entity.isDead) continue;
            if (!changed.containsKey(entity) && entity.isGlowing()) continue;
            if (!changed.containsKey(entity)) changed.put(entity, entity.isGlowing());
            entity.setGlowing(true);
        }
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) restore();
    }

    private void restore() {
        for (Map.Entry<Entity, Boolean> entry : changed.entrySet()) entry.getKey().setGlowing(entry.getValue());
        changed.clear();
    }
}

