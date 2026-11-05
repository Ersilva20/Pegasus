package dev.mylauncher.mods.fps;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_fps", name = "FPS", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class FpsMod extends SimpleTextHud {
    @Setting(name = "Show Label After", description = "\"120 FPS\" instead of \"FPS: 120\"")
    public boolean suffix = true;

    public FpsMod() {
        super("fps", "FPS", "Shows your frames per second.", 0f, 0f);
    }

    @Override
    protected String text() {
        int fps = Minecraft.getDebugFPS();
        return suffix ? fps + " FPS" : "FPS: " + fps;
    }

    @Override
    protected String sample() {
        return suffix ? "144 FPS" : "FPS: 144";
    }
}
