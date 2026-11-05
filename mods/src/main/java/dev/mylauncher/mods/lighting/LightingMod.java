package dev.mylauncher.mods.lighting;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Fullbright (high gamma) plus options that remove costly lighting effects. */
@Mod(modid = "ml_lighting", name = "Lighting Tweaks", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class LightingMod extends Feature {
    @Setting(name = "Fullbright")
    public boolean fullbright = true;
    @Setting(name = "Brightness (%)", min = 100, max = 1500, step = 50)
    public int brightness = 1000;
    @Setting(name = "Disable Entity Shadows")
    public boolean noShadows = false;

    private final KeyBinding key = MLCore.key("Toggle Fullbright", Keyboard.KEY_NONE);
    private float savedGamma = -1;
    private boolean savedShadows;

    public LightingMod() {
        super("lighting", "Lighting Tweaks", "Fullbright, and turning off entity shadows for more FPS.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        savedGamma = mc.gameSettings.gammaSetting;
        savedShadows = mc.gameSettings.entityShadows;
    }

    @Override
    protected void onDisable() {
        if (savedGamma >= 0) mc.gameSettings.gammaSetting = Math.min(1f, savedGamma);
        mc.gameSettings.entityShadows = savedShadows;
    }

    @SubscribeEvent
    public void onKey(InputEvent event) {
        if (key.isPressed()) fullbright = !fullbright;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        mc.gameSettings.gammaSetting = fullbright ? brightness / 100f : Math.min(1f, savedGamma < 0 ? 0.5f : savedGamma);
        if (noShadows) mc.gameSettings.entityShadows = false;
    }
}
