package dev.mylauncher.mods.potioneffects;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod(modid = "ml_potioneffects", name = "Potion Effects", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PotionEffectsMod extends HudFeature {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final Gui GUI = new Gui();

    @Setting(name = "Show Icons")
    public boolean icons = true;
    @Setting(name = "Show Name")
    public boolean names = true;
    @Setting(name = "Show Level")
    public boolean levels = true;
    @Setting(name = "Name Color", color = true)
    public int nameColor = 0xFFFFFFFF;
    @Setting(name = "Duration Color", color = true)
    public int durationColor = 0xFFAAAAAA;
    @Setting(name = "Blink When Expiring")
    public boolean blink = true;

    public PotionEffectsMod() {
        super("potioneffects", "Potion Effects", "Lists your active potion effects and the time left on each.", Category.HUD, false, 0f, 0.6f);
    }

    private List<PotionEffect> effects(boolean editing) {
        List<PotionEffect> out = new ArrayList<PotionEffect>();
        if (inGame()) {
            Collection<PotionEffect> c = mc.thePlayer.getActivePotionEffects();
            out.addAll(c);
        }
        if (out.isEmpty() && editing) {
            out.add(new PotionEffect(Potion.moveSpeed.id, 20 * 95, 1));
            out.add(new PotionEffect(Potion.fireResistance.id, 20 * 300, 0));
        }
        return out;
    }

    private String label(PotionEffect e) {
        String s = I18n.format(e.getEffectName());
        if (levels && e.getAmplifier() > 0) s += " " + I18n.format("enchantment.level." + (e.getAmplifier() + 1));
        return s;
    }

    @Override
    public boolean shouldRender() {
        return !effects(false).isEmpty();
    }

    @Override
    public int width() {
        int w = 0;
        for (PotionEffect e : effects(true)) {
            w = Math.max(w, mc.fontRendererObj.getStringWidth(names ? label(e) : Potion.getDurationString(e)));
        }
        return w + (icons ? 22 : 2);
    }

    @Override
    public int height() {
        return Math.max(1, effects(true).size()) * 22;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        int y = 0;
        for (PotionEffect e : effects(editing)) {
            Potion p = Potion.potionTypes[e.getPotionID()];
            int tx = icons ? 22 : 2;
            if (icons && p != null && p.hasStatusIcon()) {
                GlStateManager.color(1, 1, 1, 1);
                mc.getTextureManager().bindTexture(INVENTORY);
                int idx = p.getStatusIconIndex();
                GUI.drawTexturedModalRect(0, y + 2, (idx % 8) * 18, 198 + (idx / 8) * 18, 18, 18);
            }
            boolean expiring = e.getDuration() < 200 && blink;
            if (!expiring || (System.currentTimeMillis() / 400) % 2 == 0) {
                if (names) {
                    mc.fontRendererObj.drawString(label(e), tx, y + 2, nameColor, true);
                    mc.fontRendererObj.drawString(Potion.getDurationString(e), tx, y + 12, durationColor, true);
                } else {
                    mc.fontRendererObj.drawString(Potion.getDurationString(e), tx, y + 7, durationColor, true);
                }
            }
            y += 22;
        }
    }
}
