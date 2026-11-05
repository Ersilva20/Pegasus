package dev.mylauncher.mods.fog;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_fog", name = "Fog Customizer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class FogMod extends Feature {
    @Setting(name = "World Fog Start (%)", min = 0, max = 300, step = 5)
    public int start = 75;
    @Setting(name = "World Fog End (%)", min = 10, max = 400, step = 5)
    public int end = 100;
    @Setting(name = "Water Fog Density (%)", min = 0, max = 100, step = 5)
    public int water = 100;
    @Setting(name = "Lava Fog Density (%)", min = 0, max = 100, step = 5)
    public int lava = 100;

    public FogMod() {
        super("fog", "Fog Customizer", "Allows you to adjust the strength of fog for various fog types", Category.WORLD);
    }

    @SubscribeEvent
    public void onFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (event.block != null && event.block.getMaterial().isLiquid()) return;
        GlStateManager.setFogStart(event.farPlaneDistance * start / 100f);
        GlStateManager.setFogEnd(event.farPlaneDistance * end / 100f);
    }

    @SubscribeEvent
    public void onDensity(EntityViewRenderEvent.FogDensity event) {
        if (event.block == null) return;
        Material m = event.block.getMaterial();
        if (m == Material.water && water < 100) {
            event.density = 0.1f * water / 100f;
            GlStateManager.setFog(2048);
            event.setCanceled(true);
        } else if (m == Material.lava && lava < 100) {
            event.density = 2.0f * lava / 100f;
            GlStateManager.setFog(2048);
            event.setCanceled(true);
        }
    }
}
