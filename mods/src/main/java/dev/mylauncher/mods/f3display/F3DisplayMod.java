package dev.mylauncher.mods.f3display;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Scale and move the F3 debug screen. */
@Mod(modid = "ml_f3display", name = "Debug Screen", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class F3DisplayMod extends Feature {
    @Setting(name = "Scale", min = 0.3, max = 1.5, step = 0.05)
    public float scale = 0.75f;
    @Setting(name = "X Offset", min = 0, max = 200, step = 2)
    public int x = 0;
    @Setting(name = "Y Offset", min = 0, max = 200, step = 2)
    public int y = 0;

    private boolean pushed;

    public F3DisplayMod() {
        super("f3display", "Debug Screen", "Restyle the F3 debug screen.", Category.HUD);
    }

    @SubscribeEvent
    public void onPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.DEBUG) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        pushed = true;
    }

    @SubscribeEvent
    public void onPost(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.DEBUG && pushed) {
            GlStateManager.popMatrix();
            pushed = false;
        }
    }
}
