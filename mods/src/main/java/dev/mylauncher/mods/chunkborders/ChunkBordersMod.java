package dev.mylauncher.mods.chunkborders;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/** Toggle with F9 (change in Controls). */
@Mod(modid = "ml_chunkborders", name = "Chunk Borders", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ChunkBordersMod extends Feature {
    @Setting(name = "Current Chunk Color", color = true)
    public int current = 0xFFFFFF55;
    @Setting(name = "Neighbor Chunk Color", color = true)
    public int neighbor = 0xFFFF5555;
    @Setting(name = "Grid Color", color = true)
    public int grid = 0x6055FFFF;
    @Setting(name = "Show Inner Grid")
    public boolean showGrid = true;
    @Setting(name = "Line Width", min = 1, max = 5, step = 0.5)
    public float width = 1.5f;

    private final KeyBinding key = MLCore.key("Toggle Chunk Borders", Keyboard.KEY_F9);
    private boolean visible;

    public ChunkBordersMod() {
        super("chunkborders", "Chunk Borders", "View the border of the current chunk", Category.WORLD, true);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (key.isPressed()) visible = !visible;
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!visible || mc.thePlayer == null) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        int cx = ((int) Math.floor(mc.thePlayer.posX)) >> 4 << 4;
        int cz = ((int) Math.floor(mc.thePlayer.posZ)) >> 4 << 4;
        double minY = 0 - vy, maxY = 256 - vy;
        RenderUtil.beginWorldLines(width);
        // Neighbor chunk corners
        for (int dx = -16; dx <= 32; dx += 16) {
            for (int dz = -16; dz <= 32; dz += 16) {
                if ((dx == 0 || dx == 16) && (dz == 0 || dz == 16)) continue;
                RenderUtil.line(cx + dx - vx, minY, cz + dz - vz, cx + dx - vx, maxY, cz + dz - vz, neighbor);
            }
        }
        // Current chunk corners
        for (int dx = 0; dx <= 16; dx += 16) {
            for (int dz = 0; dz <= 16; dz += 16) {
                RenderUtil.line(cx + dx - vx, minY, cz + dz - vz, cx + dx - vx, maxY, cz + dz - vz, current);
            }
        }
        if (showGrid) {
            for (int i = 2; i < 16; i += 2) {
                RenderUtil.line(cx + i - vx, minY, cz - vz, cx + i - vx, maxY, cz - vz, grid);
                RenderUtil.line(cx + i - vx, minY, cz + 16 - vz, cx + i - vx, maxY, cz + 16 - vz, grid);
                RenderUtil.line(cx - vx, minY, cz + i - vz, cx - vx, maxY, cz + i - vz, grid);
                RenderUtil.line(cx + 16 - vx, minY, cz + i - vz, cx + 16 - vx, maxY, cz + i - vz, grid);
            }
            int py = (int) mc.thePlayer.posY;
            for (int y = Math.max(0, py - 32) & ~1; y <= Math.min(256, py + 32); y += 2) {
                double yy = y - vy;
                int c = y % 16 == 0 ? current : grid;
                RenderUtil.line(cx - vx, yy, cz - vz, cx + 16 - vx, yy, cz - vz, c);
                RenderUtil.line(cx - vx, yy, cz + 16 - vz, cx + 16 - vx, yy, cz + 16 - vz, c);
                RenderUtil.line(cx - vx, yy, cz - vz, cx - vx, yy, cz + 16 - vz, c);
                RenderUtil.line(cx + 16 - vx, yy, cz - vz, cx + 16 - vx, yy, cz + 16 - vz, c);
            }
        }
        RenderUtil.endWorldLines();
    }
}
