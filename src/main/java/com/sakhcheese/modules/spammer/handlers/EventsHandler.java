package com.sakhcheese.modules.spammer.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import com.sakhcheese.modules.spammer.PropsUtil;

public class EventsHandler {
    private World world;

    @SubscribeEvent
    public void onPlayerUpdate(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.world) {
            PropsUtil.stop();
            world = mc.world;
        }
        if (mc.world == null || mc.player == null || mc.getConnection() == null
                || !mc.getConnection().getNetworkManager().isChannelOpen()) {
            PropsUtil.stop();
            return;
        }
        String message = PropsUtil.pollMessage(System.nanoTime());
        if (message != null) mc.player.sendChatMessage(message);
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            PropsUtil.stop();
            world = null;
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Netty thread: the volatile switch prevents sends before the next client tick.
        PropsUtil.isActive = false;
    }
}
