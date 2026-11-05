package dev.mylauncher.mods.shinypots;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

/** Draws the enchantment glint over the whole slot square for potions (1.7 style "full box" shine). */
@Mod(modid = "ml_shinypots", name = "Shiny Potions", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ShinyPotsMod extends Feature implements Hooks.GuiItemOverlay {
    private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    @Setting(name = "Only Potions With Effects")
    public boolean onlyEffects = true;
    @Setting(name = "Glint Color", color = true)
    public int color = 0xFF8040CC;

    public ShinyPotsMod() {
        super("shinypots", "Shiny Potions", "Potions get the full enchanted shine.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        Hooks.guiItem.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.guiItem.remove(this);
    }

    @Override
    public void afterItem(ItemStack stack, int x, int y) {
        if (!(stack.getItem() instanceof ItemPotion)) return;
        if (onlyEffects && !stack.hasEffect()) return;
        float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(GLINT);
        GlStateManager.color(r * 0.5f, g * 0.5f, b * 0.5f, 1f);
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        for (int pass = 0; pass < 2; pass++) {
            float speed = pass == 0 ? 3000f : 4873f;
            float shift = (System.currentTimeMillis() % (long) speed) / speed * 8f;
            float angle = pass == 0 ? -50f : 10f;
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
            GlStateManager.translate(pass == 0 ? shift : -shift, 0, 0);
            GlStateManager.rotate(angle, 0, 0, 1);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            w.pos(x, y + 16, 200).tex(0, 1).endVertex();
            w.pos(x + 16, y + 16, 200).tex(1, 1).endVertex();
            w.pos(x + 16, y, 200).tex(1, 0).endVertex();
            w.pos(x, y, 200).tex(0, 0).endVertex();
            t.draw();
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }
}
