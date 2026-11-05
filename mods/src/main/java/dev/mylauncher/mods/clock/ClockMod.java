package dev.mylauncher.mods.clock;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.fml.common.Mod;

import java.text.SimpleDateFormat;
import java.util.Date;

@Mod(modid = "ml_clock", name = "Clock", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ClockMod extends SimpleTextHud {
    @Setting(name = "24 Hour Time")
    public boolean twentyFour = false;
    @Setting(name = "Show Seconds")
    public boolean seconds = false;
    @Setting(name = "Show Date")
    public boolean date = false;

    public ClockMod() {
        super("clock", "Clock", "Display the current time on the HUD.", 1f, 0f);
    }

    @Override
    protected String text() {
        String pattern = (twentyFour ? "HH:mm" : "h:mm") + (seconds ? ":ss" : "") + (twentyFour ? "" : " a");
        if (date) pattern = "dd/MM  " + pattern;
        return new SimpleDateFormat(pattern).format(new Date());
    }
}
