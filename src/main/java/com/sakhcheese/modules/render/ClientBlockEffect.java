package com.sakhcheese.modules.render;

import com.sakhcheese.core.Reference;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** A bounded, reversible client-only overlay on full support blocks. */
public final class ClientBlockEffect {
    private final Map<BlockPos, IBlockState[]> changed = new LinkedHashMap<>();
    private World world;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.world || !Reference.zamorozka || mc.player == null) restore();
        if (!Reference.zamorozka || mc.world == null || mc.player == null || !mc.world.isRemote) return;
        world = mc.world;
        if (!mc.inGameHasFocus || !mc.player.onGround) return;
        AxisAlignedBB feet = mc.player.getEntityBoundingBox();
        int y = MathHelper.floor(feet.minY - 0.01);
        for (int x = MathHelper.floor(feet.minX + 0.001); x <= MathHelper.floor(feet.maxX - 0.001); x++) {
            for (int z = MathHelper.floor(feet.minZ + 0.001); z <= MathHelper.floor(feet.maxZ - 0.001); z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (changed.containsKey(pos) || changed.size() >= 4096 || !world.isBlockLoaded(pos)) continue;
                IBlockState original = world.getBlockState(pos);
                if (!original.isFullCube() || original.getBlock().hasTileEntity(original)) continue;
                IBlockState replacement = (pos.hashCode() % 3 == 0 ? Blocks.DIRT : Blocks.BROWN_MUSHROOM_BLOCK).getDefaultState();
                changed.put(pos, new IBlockState[] {original, replacement});
                world.setBlockState(pos, replacement, 2);
            }
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld() == world) restore();
    }

    private void restore() {
        if (world != null) {
            for (Map.Entry<BlockPos, IBlockState[]> entry : changed.entrySet()) {
                // Respect authoritative block updates and avoid loading unloaded chunks.
                if (world.isBlockLoaded(entry.getKey()) && world.getBlockState(entry.getKey()) == entry.getValue()[1]) {
                    world.setBlockState(entry.getKey(), entry.getValue()[0], 2);
                }
            }
        }
        changed.clear();
        world = null;
    }
}
