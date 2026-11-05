package dev.mylauncher.mods.tntcountdown;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_tntcountdown", name = "TNT Countdown", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class TntCountdownMod extends Feature {
    @Setting(name = "Decimals", min = 0, max = 2, step = 1)
    public int decimals = 2;
    @Setting(name = "Scale", min = 0.5, max = 3, step = 0.1)
    public float scale = 1.2f;

    public TntCountdownMod() {
        super("tntcountdown", "TNT Countdown", "Displays the fuse countdown above TNT.", Category.WORLD);
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        float pt = event.partialTicks;
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityTNTPrimed)) continue;
            EntityTNTPrimed tnt = (EntityTNTPrimed) e;
            float secs = Math.max(0, (tnt.fuse - pt) / 20f);
            float pct = Math.min(1f, secs / 4f);
            int r = (int) (255 * (1 - pct)), g = (int) (255 * pct);
            int color = 0xFF000000 | (r << 16) | (g << 8);
            double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt - vx;
            double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt - vy + 1.4;
            double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt - vz;
            RenderUtil.worldLabel(String.format("%." + decimals + "f", secs), x, y, z, color, scale, true);
        }
    }
}
