package dev.mylauncher.mods.memory;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_memory", name = "Memory Usage", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MemoryMod extends SimpleTextHud {
    public enum Format { PERCENT, USED_TOTAL, BOTH }

    @Setting(name = "Format")
    public Format format = Format.BOTH;

    public MemoryMod() {
        super("memory", "Memory Usage", "Shows how much RAM the game is using.", 0f, 0.12f);
    }

    @Override
    protected String text() {
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory() / 1048576L;
        long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        long pct = used * 100L / Math.max(1, max);
        switch (format) {
            case PERCENT:
                return "Mem: " + pct + "%";
            case USED_TOTAL:
                return used + "/" + max + " MB";
            default:
                return "Mem: " + pct + "% (" + used + "MB)";
        }
    }
}
