package dev.mylauncher.mods.titles;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.mixin.GuiIngameAccessor;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_titles", name = "Titles", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class TitlesMod extends Feature implements Hooks.OverlayRenderer {
    @Setting(name = "Hide Titles")
    public boolean hide = false;
    @Setting(name = "Title Scale", min = 0.5, max = 6, step = 0.25)
    public float titleScale = 4f;
    @Setting(name = "Subtitle Scale", min = 0.5, max = 4, step = 0.25)
    public float subtitleScale = 2f;
    @Setting(name = "Vertical Offset", min = -150, max = 150, step = 5)
    public int offset = 0;
    @Setting(name = "Text Shadow")
    public boolean shadow = true;

    public TitlesMod() {
        super("titles", "Titles", "Adjust the minecraft title and subtitle scale and location.", Category.HUD);
    }

    @Override
    protected void onEnable() {
        Hooks.title = this;
    }

    @Override
    protected void onDisable() {
        if (Hooks.title == this) Hooks.title = null;
    }

    @Override
    public boolean render(GuiIngame gui, int width, int height, float partialTicks) {
        GuiIngameAccessor a = (GuiIngameAccessor) gui;
        int timer = a.ml$getTitlesTimer();
        if (hide || timer <= 0) return true;
        int in = a.ml$getTitleFadeIn(), stay = a.ml$getTitleDisplayTime(), out = a.ml$getTitleFadeOut();
        float age = timer - partialTicks;
        int opacity = 255;
        if (timer > out + stay) opacity = (int) ((in + stay + out - age) * 255f / in);
        if (timer <= out) opacity = (int) (age * 255f / out);
        opacity = MathHelper.clamp_int(opacity, 0, 255);
        if (opacity <= 8) return true;
        int alpha = opacity << 24 & 0xFF000000;
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height / 2f + offset, 0);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        GlStateManager.scale(titleScale, titleScale, titleScale);
        String t = a.ml$getTitle();
        if (t != null) mc.fontRendererObj.drawString(t, -mc.fontRendererObj.getStringWidth(t) / 2f, -10f, 0xFFFFFF | alpha, shadow);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(subtitleScale, subtitleScale, subtitleScale);
        String s = a.ml$getSubTitle();
        if (s != null) mc.fontRendererObj.drawString(s, -mc.fontRendererObj.getStringWidth(s) / 2f, 5f, 0xFFFFFF | alpha, shadow);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        return true;
    }
}
