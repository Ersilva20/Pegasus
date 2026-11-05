package dev.mylauncher.mods.damagetint;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Red vignette at the screen edges that gets stronger as your health drops. */
@Mod(modid = "ml_damagetint", name = "Damage Tint", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class DamageTintMod extends Feature {
    @Setting(name = "Start Below Health", min = 1, max = 20, step = 1)
    public int threshold = 10;
    @Setting(name = "Max Opacity (%)", min = 10, max = 100, step = 5)
    public int maxOpacity = 60;
    @Setting(name = "Color", color = true)
    public int color = 0xFFFF0000;
    @Setting(name = "Edge Size (%)", min = 5, max = 50, step = 1)
    public int edge = 25;

    public DamageTintMod() {
        super("damagetint", "Damage Tint", "Adds a tint to the screen when you are low on health.", Category.PVP);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || mc.thePlayer == null) return;
        float hp = mc.thePlayer.getHealth();
        if (hp >= threshold || mc.thePlayer.capabilities.isCreativeMode) return;
        float strength = 1f - hp / threshold;
        int a = (int) (255 * strength * maxOpacity / 100f);
        int w = event.resolution.getScaledWidth(), h = event.resolution.getScaledHeight();
        int ex = w * edge / 100, ey = h * edge / 100;
        float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float A = a / 255f;
        // left, right, top, bottom gradients
        quad(wr, 0, 0, ex, h, r, g, b, A, 0, true);
        quad(wr, w - ex, 0, w, h, r, g, b, 0, A, true);
        quad(wr, 0, 0, w, ey, r, g, b, A, 0, false);
        quad(wr, 0, h - ey, w, h, r, g, b, 0, A, false);
        t.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
    }

    private static void quad(WorldRenderer wr, float l, float tp, float rt, float bt, float r, float g, float b,
                             float a1, float a2, boolean horizontal) {
        if (horizontal) {
            wr.pos(l, bt, 0).color(r, g, b, a1).endVertex();
            wr.pos(rt, bt, 0).color(r, g, b, a2).endVertex();
            wr.pos(rt, tp, 0).color(r, g, b, a2).endVertex();
            wr.pos(l, tp, 0).color(r, g, b, a1).endVertex();
        } else {
            wr.pos(l, bt, 0).color(r, g, b, a2).endVertex();
            wr.pos(rt, bt, 0).color(r, g, b, a2).endVertex();
            wr.pos(rt, tp, 0).color(r, g, b, a1).endVertex();
            wr.pos(l, tp, 0).color(r, g, b, a1).endVertex();
        }
    }
}
