package dev.mylauncher.mods.minimap;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Top-down terrain map around you built from block map colors, with player/mob dots. */
@Mod(modid = "ml_minimap", name = "Minimap", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MinimapMod extends HudFeature {
    private static final int RES = 128;

    @Setting(name = "Size", min = 48, max = 200, step = 4)
    public int size = 96;
    @Setting(name = "Zoom (blocks per pixel)", min = 0.5, max = 4, step = 0.25)
    public float zoom = 1f;
    @Setting(name = "Rotate With Player")
    public boolean rotate = true;
    @Setting(name = "Show Players")
    public boolean players = true;
    @Setting(name = "Show Hostile Mobs")
    public boolean mobs = false;
    @Setting(name = "Show Coordinates")
    public boolean coords = true;
    @Setting(name = "Height Shading")
    public boolean shading = true;
    @Setting(name = "Update Every (ticks)", min = 1, max = 40, step = 1)
    public int updateRate = 10;

    private DynamicTexture texture;
    private ResourceLocation location;
    private int ticks;
    private double centerX, centerZ;

    public MinimapMod() {
        super("minimap", "Minimap", "A small map of the area around you.", Category.HUD, false, 1f, 0f);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !inGame()) return;
        if (texture == null) {
            texture = new DynamicTexture(RES, RES);
            location = mc.getTextureManager().getDynamicTextureLocation("ml_minimap", texture);
        }
        if (++ticks % updateRate != 0) return;
        int[] px = texture.getTextureData();
        centerX = mc.thePlayer.posX;
        centerZ = mc.thePlayer.posZ;
        for (int iz = 0; iz < RES; iz++) {
            for (int ix = 0; ix < RES; ix++) {
                int wx = (int) Math.floor(centerX + (ix - RES / 2) * zoom);
                int wz = (int) Math.floor(centerZ + (iz - RES / 2) * zoom);
                px[iz * RES + ix] = color(wx, wz);
            }
        }
        texture.updateDynamicTexture();
    }

    private int height(int x, int z) {
        return mc.theWorld.getHeight(new BlockPos(x, 0, z)).getY();
    }

    private int color(int x, int z) {
        BlockPos col = new BlockPos(x, 0, z);
        if (!mc.theWorld.isBlockLoaded(col)) return 0xFF101010;
        int h = height(x, z);
        BlockPos pos = new BlockPos(x, h - 1, z);
        IBlockState state = mc.theWorld.getBlockState(pos);
        int y = h - 1;
        while (y > 0 && state.getBlock().getMapColor(state) == MapColor.airColor) {
            y--;
            pos = pos.down();
            state = mc.theWorld.getBlockState(pos);
        }
        int c = state.getBlock().getMapColor(state).colorValue;
        if (shading) {
            int north = height(x, z - 1);
            float f = h > north ? 1.12f : h < north ? 0.82f : 1f;
            int r = Math.min(255, (int) (((c >> 16) & 0xFF) * f));
            int g = Math.min(255, (int) (((c >> 8) & 0xFF) * f));
            int b = Math.min(255, (int) ((c & 0xFF) * f));
            c = (r << 16) | (g << 8) | b;
        }
        return 0xFF000000 | c;
    }

    @Override
    public int width() {
        return size;
    }

    @Override
    public int height() {
        return size + (coords ? 11 : 0);
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        Gui.drawRect(-1, -1, size + 1, size + 1, 0xFF000000);
        if (texture == null || !inGame()) {
            Gui.drawRect(0, 0, size, size, 0xFF2A4A2A);
            return;
        }
        // Scissor so a rotated map stays inside the square.
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        int sx = Math.round(screenX(sr) * sf), sy = Math.round(screenY(sr) * sf);
        int sw = Math.round(size * scale * sf);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, mc.displayHeight - sy - sw, sw, sw);

        GlStateManager.pushMatrix();
        GlStateManager.translate(size / 2f, size / 2f, 0);
        if (rotate) GlStateManager.rotate(180f - mc.thePlayer.rotationYaw, 0, 0, 1);
        // Offset for movement since the last texture update
        // The texture covers RES*zoom blocks; draw it 1.5x larger than the view so a rotated map fills the corners.
        float pxPerBlock = size * 1.5f / (RES * zoom);
        int half = Math.round(size * 0.75f);
        GlStateManager.translate((float) (centerX - mc.thePlayer.posX) * pxPerBlock, (float) (centerZ - mc.thePlayer.posZ) * pxPerBlock, 0);
        mc.getTextureManager().bindTexture(location);
        GlStateManager.color(1, 1, 1, 1);
        Gui.drawModalRectWithCustomSizedTexture(-half, -half, 0, 0, half * 2, half * 2, half * 2, half * 2);

        if (players || mobs) {
            for (Entity e : mc.theWorld.loadedEntityList) {
                boolean isPlayer = e instanceof EntityPlayer && e != mc.thePlayer;
                boolean isMob = e instanceof IMob;
                if (!(players && isPlayer) && !(mobs && isMob)) continue;
                float ex = (float) (e.posX - centerX) * pxPerBlock, ez = (float) (e.posZ - centerZ) * pxPerBlock;
                int c = isPlayer ? 0xFFFFFFFF : 0xFFFF3333;
                RenderUtil.rect(ex - 1.5f, ez - 1.5f, ex + 1.5f, ez + 1.5f, c);
            }
        }
        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Player arrow at center
        GlStateManager.pushMatrix();
        GlStateManager.translate(size / 2f, size / 2f, 0);
        if (!rotate) GlStateManager.rotate(mc.thePlayer.rotationYaw + 180f, 0, 0, 1);
        RenderUtil.rect(-1, -3, 1, 3, 0xFFFF5555);
        RenderUtil.rect(-2, -1, 2, 1, 0xFFFF5555);
        GlStateManager.popMatrix();
        double angle = rotate ? Math.toRadians(180f - mc.thePlayer.rotationYaw) : 0;
        float r = size / 2f - 6;
        float nx = (float) (size / 2f + Math.sin(angle) * r), ny = (float) (size / 2f - Math.cos(angle) * r);
        mc.fontRendererObj.drawStringWithShadow("N", nx - 2.5f, ny - 4, 0xFFFFFFFF);
        if (coords) {
            String c = (int) Math.floor(mc.thePlayer.posX) + ", " + (int) Math.floor(mc.thePlayer.posY) + ", " + (int) Math.floor(mc.thePlayer.posZ);
            Gui.drawRect(-1, size + 1, size + 1, size + 11, 0x90000000);
            mc.fontRendererObj.drawString(c, (size - mc.fontRendererObj.getStringWidth(c)) / 2, size + 2, 0xFFFFFFFF, true);
        }
    }
}
