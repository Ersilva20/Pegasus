package dev.mylauncher.mods.nametags;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Restyles player nametags and can show your own nametag in third person. */
@Mod(modid = "ml_nametags", name = "Nametags", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class NametagsMod extends Feature {
    @Setting(name = "Show Own Nametag (F5)")
    public boolean showOwn = true;
    @Setting(name = "Custom Style for Players")
    public boolean custom = true;
    @Setting(name = "Text Shadow")
    public boolean shadow = true;
    @Setting(name = "Background Opacity (%)", min = 0, max = 100, step = 5)
    public int background = 25;
    @Setting(name = "Scale", min = 0.5, max = 2, step = 0.05)
    public float scale = 1f;
    @Setting(name = "Max Distance", min = 8, max = 128, step = 4)
    public int maxDistance = 64;

    public NametagsMod() {
        super("nametags", "Nametags", "A mod that allows you to modify nametags.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onName(RenderLivingEvent.Specials.Pre<?> event) {
        if (!(event.entity instanceof EntityPlayer) || !custom) return;
        EntityPlayer p = (EntityPlayer) event.entity;
        if (p == mc.thePlayer) return;
        event.setCanceled(true);
        if (p.isInvisibleToPlayer(mc.thePlayer) || p.riddenByEntity != null) return;
        if (p.getDistanceSqToEntity(mc.thePlayer) > maxDistance * maxDistance) return;
        draw(p, event.x, event.y, event.z);
    }

    @SubscribeEvent
    public void onOwn(RenderLivingEvent.Specials.Post<?> event) {
        if (event.entity != mc.thePlayer || !showOwn || mc.gameSettings.thirdPersonView == 0) return;
        draw(mc.thePlayer, event.x, event.y, event.z);
    }

    private void draw(EntityPlayer p, double x, double y, double z) {
        String name = p.getDisplayName().getFormattedText();
        FontRenderer fr = mc.fontRendererObj;
        boolean sneaking = p.isSneaking();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + p.height + 0.5f, (float) z);
        GL11.glNormal3f(0, 1, 0);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.rotate(mc.getRenderManager().playerViewX * (mc.gameSettings.thirdPersonView == 2 ? -1 : 1), 1, 0, 0);
        float s = 0.02666667f * scale;
        GlStateManager.scale(-s, -s, s);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (!sneaking) GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int half = fr.getStringWidth(name) / 2;
        if (background > 0) {
            GlStateManager.disableTexture2D();
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            float a = background / 100f;
            w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            w.pos(-half - 1, -1, 0).color(0, 0, 0, a).endVertex();
            w.pos(-half - 1, 8, 0).color(0, 0, 0, a).endVertex();
            w.pos(half + 1, 8, 0).color(0, 0, 0, a).endVertex();
            w.pos(half + 1, -1, 0).color(0, 0, 0, a).endVertex();
            t.draw();
            GlStateManager.enableTexture2D();
        }
        int color = sneaking ? 0x20FFFFFF : 0x20FFFFFF;
        if (!sneaking) fr.drawString(name, -half, 0, color, false);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        fr.drawString(name, -half, 0, sneaking ? 0x80FFFFFF : 0xFFFFFFFF, shadow);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
