package dev.mylauncher.mods.momentum;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_momentum", name = "Momentum", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MomentumMod extends SimpleTextHud {
    @Setting(name = "Include Vertical Speed")
    public boolean vertical = false;
    @Setting(name = "Decimals", min = 0, max = 3, step = 1)
    public int decimals = 2;

    public MomentumMod() {
        super("momentum", "Momentum", "Shows how fast you are moving, in blocks per second.", 0f, 0.42f);
    }

    @Override
    protected String text() {
        double dx = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double dy = mc.thePlayer.posY - mc.thePlayer.lastTickPosY;
        double dz = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        double speed = Math.sqrt(dx * dx + dz * dz + (vertical ? dy * dy : 0)) * 20.0;
        return String.format("%." + decimals + "f m/s", speed);
    }

    @Override
    protected String sample() {
        return "5.61 m/s";
    }
}
