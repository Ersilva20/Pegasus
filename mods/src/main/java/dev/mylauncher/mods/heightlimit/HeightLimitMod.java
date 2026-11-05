package dev.mylauncher.mods.heightlimit;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Shows how many blocks you can still build up and draws a plane at the limit. */
@Mod(modid = "ml_heightlimit", name = "Height Limit", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HeightLimitMod extends SimpleTextHud {
    @Setting(name = "Height Limit (Y)", description = "256 = vanilla build limit; set the map limit for minigames", min = 1, max = 256, step = 1)
    public int limit = 256;
    @Setting(name = "Show Overlay Plane")
    public boolean plane = true;
    @Setting(name = "Overlay Within (blocks)", min = 2, max = 32, step = 1)
    public int within = 8;
    @Setting(name = "Overlay Color", color = true)
    public int planeColor = 0x40FF3333;
    @Setting(name = "Overlay Radius", min = 2, max = 16, step = 1)
    public int radius = 6;

    public HeightLimitMod() {
        super("heightlimit", "Height Limit", "Block overlay and HUD display to help you build up to map height limits", 0.5f, 0.7f);
    }

    @Override
    protected String text() {
        int left = limit - (int) Math.floor(mc.thePlayer.posY);
        return left > 50 ? null : "Height limit in " + Math.max(0, left);
    }

    @Override
    protected String sample() {
        return "Height limit in 7";
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!plane || mc.thePlayer == null) return;
        if (limit - mc.thePlayer.posY > within) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        int px = (int) Math.floor(mc.thePlayer.posX), pz = (int) Math.floor(mc.thePlayer.posZ);
        AxisAlignedBB bb = new AxisAlignedBB(px - radius, limit, pz - radius, px + radius + 1, limit + 0.01, pz + radius + 1).offset(-vx, -vy, -vz);
        RenderUtil.beginWorldLines(2f);
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        RenderUtil.boxFilled(bb, planeColor);
        RenderUtil.boxOutline(bb, planeColor | 0xFF000000);
        RenderUtil.endWorldLines();
    }
}
