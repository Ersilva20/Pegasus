package dev.mylauncher.core.hud;

import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.gui.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Draws every enabled HUD feature after the vanilla HUD. */
public final class HudRenderer {
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof HudEditorScreen) return;
        renderAll(event.resolution, event.partialTicks, false);
    }

    public static void renderAll(ScaledResolution sr, float pt, boolean editing) {
        for (Feature f : MLCore.features()) {
            if (!(f instanceof HudFeature) || !f.isEnabled()) continue;
            HudFeature h = (HudFeature) f;
            try {
                if (!editing && !h.shouldRender()) continue;
                draw(h, sr, pt, editing);
            } catch (Exception e) {
                MLCore.LOG.error("HUD " + h.id + " failed to render", e);
            }
        }
    }

    public static void draw(HudFeature h, ScaledResolution sr, float pt, boolean editing) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(h.screenX(sr), h.screenY(sr), 0);
        GlStateManager.scale(h.scale, h.scale, 1);
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        h.render(pt, editing);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
