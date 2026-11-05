package dev.mylauncher.mods.viewangles;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.TextHud;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod(modid = "ml_viewangles", name = "View Angles", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ViewAnglesMod extends TextHud {
    @Setting(name = "Decimals", min = 0, max = 3, step = 1)
    public int decimals = 1;
    @Setting(name = "Show Altitude")
    public boolean altitude = false;
    @Setting(name = "Show Speed")
    public boolean speed = false;

    public ViewAnglesMod() {
        super("viewangles", "View Angles", "Shows your yaw and pitch, plus speed and height if you want.", Category.HUD, false, 0f, 0.5f);
        fixedWidth = false;
    }

    @Override
    protected List<String> lines() {
        String f = "%." + decimals + "f";
        List<String> out = new ArrayList<String>();
        out.add("Yaw: " + String.format(f, MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw)));
        out.add("Pitch: " + String.format(f, mc.thePlayer.rotationPitch));
        if (altitude) out.add("Altitude: " + String.format(f, mc.thePlayer.posY));
        if (speed) {
            double dx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX, dz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
            out.add("Speed: " + String.format(f, Math.sqrt(dx * dx + dz * dz) * 20) + " m/s");
        }
        return out;
    }

    @Override
    protected List<String> sampleLines() {
        return Arrays.asList("Yaw: 90.0", "Pitch: 12.5");
    }
}
