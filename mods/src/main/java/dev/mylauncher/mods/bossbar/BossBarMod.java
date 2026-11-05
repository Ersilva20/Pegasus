package dev.mylauncher.mods.bossbar;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Movable boss bar (Wither / Ender Dragon / server boss bars). */
@Mod(modid = "ml_bossbar", name = "Boss Bar", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class BossBarMod extends HudFeature {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final Gui GUI = new Gui();

    @Setting(name = "Hide Boss Bar")
    public boolean hide = false;
    @Setting(name = "Show Name")
    public boolean showName = true;
    @Setting(name = "Show Bar")
    public boolean showBar = true;

    public BossBarMod() {
        super("bossbar", "Boss Bar", "Allows you to hide, move or customize the Minecraft boss bar.", Category.HUD, false, 0.5f, 0.02f);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH) event.setCanceled(true);
    }

    @Override
    public boolean shouldRender() {
        return !hide && BossStatus.bossName != null && BossStatus.statusBarTime > 0;
    }

    @Override
    public int width() {
        return 182;
    }

    @Override
    public int height() {
        return 18;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        String name = BossStatus.bossName != null && BossStatus.statusBarTime > 0 ? BossStatus.bossName : "Wither";
        float health = BossStatus.statusBarTime > 0 ? BossStatus.healthScale : 0.7f;
        if (showBar) {
            mc.getTextureManager().bindTexture(ICONS);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableBlend();
            int filled = (int) (health * 183f);
            GUI.drawTexturedModalRect(0, 12, 0, 74, 182, 5);
            GUI.drawTexturedModalRect(0, 12, 0, 74, 182, 5);
            if (filled > 0) GUI.drawTexturedModalRect(0, 12, 0, 79, filled, 5);
        }
        if (showName) {
            mc.fontRendererObj.drawStringWithShadow(name, 91 - mc.fontRendererObj.getStringWidth(name) / 2f, 2, 0xFFFFFFFF);
        }
        GlStateManager.color(1, 1, 1, 1);
    }
}
