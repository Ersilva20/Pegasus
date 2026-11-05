package dev.mylauncher.mods.daycounter;

import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_daycounter", name = "Day Counter", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class DayCounterMod extends SimpleTextHud {
    public DayCounterMod() {
        super("daycounter", "Day Counter", "View the number of Minecraft Days you have played in a world.", 1f, 0.06f);
    }

    @Override
    protected String text() {
        return "Day " + (mc.theWorld.getWorldTime() / 24000L + 1);
    }

    @Override
    protected String sample() {
        return "Day 12";
    }
}
