package dev.mylauncher.mods.actionbar;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.mixin.GuiIngameAccessor;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.Mod;

import java.awt.Color;

@Mod(modid = "ml_actionbar", name = "Action Bar", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ActionBarMod extends Feature implements Hooks.OverlayRenderer {
    @Setting(name = "Hide Action Bar")
    public boolean hide = false;
    @Setting(name = "Scale", min = 0.5, max = 3, step = 0.1)
    public float scale = 1f;
    @Setting(name = "Vertical Offset", min = -200, max = 60, step = 2)
    public int offset = 0;
    @Setting(name = "Text Shadow")
    public boolean shadow = false;

    public ActionBarMod() {
        super("actionbar", "Action Bar", "Move and resize the action bar text above the hotbar.", Category.HUD);
    }

    @Override
    protected void onEnable() {
        Hooks.actionBar = this;
    }

    @Override
    protected void onDisable() {
        if (Hooks.actionBar == this) Hooks.actionBar = null;
    }

    @Override
    public boolean render(GuiIngame gui, int width, int height, float partialTicks) {
        GuiIngameAccessor a = (GuiIngameAccessor) gui;
        int upFor = a.ml$getRecordPlayingUpFor();
        String text = a.ml$getRecordPlaying();
        if (hide || upFor <= 0 || text == null) return true;
        float hue = upFor - partialTicks;
        int opacity = Math.min(255, (int) (hue * 256f / 20f));
        if (opacity <= 0) return true;
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, height - 68 + offset, 0);
        GlStateManager.scale(scale, scale, 1);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int color = a.ml$getRecordIsPlaying() ? Color.HSBtoRGB(hue / 50f, 0.7f, 0.6f) & 0xFFFFFF : 0xFFFFFF;
        mc.fontRendererObj.drawString(text, -mc.fontRendererObj.getStringWidth(text) / 2f, -4, color | (opacity << 24), shadow);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        return true;
    }
}
