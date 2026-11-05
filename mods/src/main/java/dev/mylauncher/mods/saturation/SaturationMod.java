package dev.mylauncher.mods.saturation;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** AppleSkin-style saturation overlay on the hunger bar + optional HUD text. */
@Mod(modid = "ml_saturation", name = "Saturation", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class SaturationMod extends SimpleTextHud {
    @Setting(name = "Saturation Overlay on Hunger Bar")
    public boolean overlay = true;
    @Setting(name = "Preview Held Food")
    public boolean preview = true;
    @Setting(name = "Overlay Color", color = true)
    public int overlayColor = 0x90FFD700;
    @Setting(name = "Show HUD Text")
    public boolean showHud = true;

    public SaturationMod() {
        super("saturation", "Saturation", "Saturation HUD with AppleSkin features", 1f, 0.3f);
    }

    @Override
    public boolean shouldRender() {
        return showHud && super.shouldRender();
    }

    @Override
    protected String text() {
        return String.format("Saturation: %.1f", mc.thePlayer.getFoodStats().getSaturationLevel());
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (!overlay || event.type != RenderGameOverlayEvent.ElementType.FOOD) return;
        if (!mc.playerController.shouldDrawHUD()) return;
        int right = event.resolution.getScaledWidth() / 2 + 91;
        int top = event.resolution.getScaledHeight() - GuiIngameForge.right_height + 10;
        float sat = mc.thePlayer.getFoodStats().getSaturationLevel();
        drawUnits(right, top, sat, overlayColor);

        ItemStack held = mc.thePlayer.getHeldItem();
        if (preview && held != null && held.getItem() instanceof ItemFood) {
            ItemFood food = (ItemFood) held.getItem();
            int hunger = mc.thePlayer.getFoodStats().getFoodLevel();
            int gain = food.getHealAmount(held);
            float satGain = gain * food.getSaturationModifier(held) * 2f;
            float newSat = Math.min(sat + satGain, Math.min(20, hunger + gain));
            int alpha = (int) (80 + 60 * Math.sin(System.currentTimeMillis() / 200.0));
            drawUnits(right, top, newSat, (alpha << 24) | 0xFFFFFF);
        }
    }

    /** Each hunger icon is 2 points; draw a bar over the bottom of each icon. */
    private void drawUnits(int right, int top, float amount, int color) {
        for (int i = 0; i < 10; i++) {
            float v = amount - i * 2;
            if (v <= 0) break;
            int x = right - i * 8 - 9;
            float frac = Math.min(1f, v / 2f);
            int w = Math.max(1, Math.round(7 * frac));
            Gui.drawRect(x + 8 - w, top + 7, x + 8, top + 9, color);
        }
    }
}
