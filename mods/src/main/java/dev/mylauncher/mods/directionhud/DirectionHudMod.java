package dev.mylauncher.mods.directionhud;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Mod;

/** A compass strip like the one at the top of FPS games. */
@Mod(modid = "ml_directionhud", name = "Compass", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class DirectionHudMod extends HudFeature {
    private static final String[] LABELS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    @Setting(name = "Width", min = 80, max = 300, step = 10)
    public int stripWidth = 150;
    @Setting(name = "Background")
    public boolean background = true;
    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0x6F000000;
    @Setting(name = "Text Color", color = true)
    public int textColor = 0xFFFFFFFF;
    @Setting(name = "Marker Color", color = true)
    public int markerColor = 0xFFFF5555;
    @Setting(name = "Show Degrees")
    public boolean degrees = true;

    public DirectionHudMod() {
        super("directionhud", "Compass", "A compass strip showing which way you are facing.", Category.HUD, false, 0.5f, 0f);
    }

    @Override
    public int width() {
        return stripWidth;
    }

    @Override
    public int height() {
        return degrees ? 24 : 14;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        float yaw = inGame() ? MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) : 0;
        int w = stripWidth;
        if (background) Gui.drawRect(0, 0, w, 14, backgroundColor);
        float pxPerDeg = w / 180f;
        for (int i = 0; i < 16; i++) {
            float angle = i * 22.5f;
            float diff = MathHelper.wrapAngleTo180_float(angle - yaw);
            if (Math.abs(diff) > 90) continue;
            int x = Math.round(w / 2f + diff * pxPerDeg);
            if (i % 2 == 0) {
                String label = LABELS[i / 2];
                mc.fontRendererObj.drawString(label, x - mc.fontRendererObj.getStringWidth(label) / 2, 3, label.length() == 1 ? textColor : 0xFFAAAAAA, true);
            } else {
                Gui.drawRect(x, 5, x + 1, 9, 0x90FFFFFF);
            }
        }
        Gui.drawRect(w / 2, 0, w / 2 + 1, 14, markerColor);
        if (degrees) {
            float d = (yaw + 360) % 360;
            String s = String.format("%.0f°", d);
            mc.fontRendererObj.drawString(s, w / 2 - mc.fontRendererObj.getStringWidth(s) / 2, 15, textColor, true);
        }
    }
}
