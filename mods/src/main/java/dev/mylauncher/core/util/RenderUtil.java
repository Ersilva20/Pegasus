package dev.mylauncher.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/** Small drawing helpers shared by every mod. */
public final class RenderUtil {
    private RenderUtil() {
    }

    public static void rect(float left, float top, float right, float bottom, int color) {
        if (left < right && top < bottom) {
            Gui.drawRect(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom), color);
        }
    }

    /** Rectangle with 1px-trimmed corners, which reads as slightly rounded at GUI scale. */
    public static void softRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left + 1, top, right - 1, bottom, color);
        Gui.drawRect(left, top + 1, left + 1, bottom - 1, color);
        Gui.drawRect(right - 1, top + 1, right, bottom - 1, color);
    }

    public static void outline(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    public static void item(ItemStack stack, int x, int y, boolean overlay) {
        if (stack == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        if (overlay) mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static void glColor(int argb) {
        GlStateManager.color(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f);
    }

    /** Rainbow color cycling with time (used by chroma options). */
    public static int chroma(float offset) {
        float hue = ((System.currentTimeMillis() % 4000L) / 4000f + offset) % 1f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.8f, 1f) & 0xFFFFFF);
    }

    // ---- World-space drawing (call inside RenderWorldLastEvent, positions relative to camera) ----

    public static void beginWorldLines(float width) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(width);
    }

    public static void endWorldLines() {
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void boxOutline(AxisAlignedBB bb, int color) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        glColor(color);
        w.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        w.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        t.draw();
        w.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        w.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        t.draw();
        w.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        w.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        t.draw();
    }

    public static void boxFilled(AxisAlignedBB bb, int color) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        glColor(color);
        w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        // bottom
        w.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        // top
        w.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        // north
        w.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        // south
        w.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        // west
        w.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        // east
        w.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        w.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        w.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        t.draw();
    }

    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        glColor(color);
        w.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        w.pos(x1, y1, z1).endVertex();
        w.pos(x2, y2, z2).endVertex();
        t.draw();
    }

    /** Draws a nametag-style billboard label at a camera-relative position. */
    public static void worldLabel(String text, double x, double y, double z, int color, float scale, boolean background) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0, 1, 0);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.rotate(mc.getRenderManager().playerViewX * (mc.gameSettings.thirdPersonView == 2 ? -1 : 1), 1, 0, 0);
        float s = 0.0266f * scale;
        GlStateManager.scale(-s, -s, s);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int w = mc.fontRendererObj.getStringWidth(text) / 2;
        if (background) {
            GlStateManager.disableTexture2D();
            Gui.drawRect(-w - 2, -2, w + 2, 9, 0x50000000);
            GlStateManager.enableTexture2D();
        }
        mc.fontRendererObj.drawString(text, -w, 0, color, true);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
