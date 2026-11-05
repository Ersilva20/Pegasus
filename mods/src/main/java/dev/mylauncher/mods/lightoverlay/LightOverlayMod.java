package dev.mylauncher.mods.lightoverlay;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Toggle with F7. Red X = mobs can spawn now (block light and sky light both low),
 * yellow X = mobs can spawn at night (only block light is low).
 */
@Mod(modid = "ml_lightoverlay", name = "Light Levels", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class LightOverlayMod extends Feature {
    @Setting(name = "Radius", min = 4, max = 32, step = 1)
    public int radius = 16;
    @Setting(name = "Show Night-Only Spots (yellow)")
    public boolean nightSpots = true;

    private final KeyBinding key = MLCore.key("Toggle Light Levels", Keyboard.KEY_F7);
    private boolean visible;
    private final List<long[]> spots = new ArrayList<long[]>();
    private int ticks;

    public LightOverlayMod() {
        super("lightoverlay", "Light Levels", "Marks the blocks where mobs can spawn in the dark.", Category.WORLD, true);
    }

    @SubscribeEvent
    public void onKey(InputEvent event) {
        if (key.isPressed()) visible = !visible;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !visible || mc.theWorld == null || mc.thePlayer == null) return;
        if (++ticks % 10 != 0) return;
        spots.clear();
        BlockPos center = new BlockPos(mc.thePlayer);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -8; dy <= 4; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (!mc.theWorld.isBlockLoaded(pos)) continue;
                    Block here = mc.theWorld.getBlockState(pos).getBlock();
                    if (here.isNormalCube() || here.getMaterial().isLiquid() || here.getMaterial().isSolid()) continue;
                    BlockPos below = pos.down();
                    Block ground = mc.theWorld.getBlockState(below).getBlock();
                    if (!ground.isSideSolid(mc.theWorld, below, EnumFacing.UP) || ground.getMaterial().isLiquid()) continue;
                    int block = mc.theWorld.getLightFor(EnumSkyBlock.BLOCK, pos);
                    if (block >= 8) continue;
                    int sky = mc.theWorld.getLightFor(EnumSkyBlock.SKY, pos);
                    boolean now = sky < 8;
                    if (!now && !nightSpots) continue;
                    spots.add(new long[]{pos.getX(), pos.getY(), pos.getZ(), now ? 1 : 0});
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!visible || spots.isEmpty()) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        RenderUtil.beginWorldLines(2f);
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        for (long[] s : spots) {
            double x = s[0] - vx, y = s[1] - vy + 0.02, z = s[2] - vz;
            int c = s[3] == 1 ? 0xFFFF3333 : 0xFFFFFF33;
            RenderUtil.line(x + 0.1, y, z + 0.1, x + 0.9, y, z + 0.9, c);
            RenderUtil.line(x + 0.9, y, z + 0.1, x + 0.1, y, z + 0.9, c);
        }
        RenderUtil.endWorldLines();
    }
}
