package dev.mylauncher.mods.stopwatch;

import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/** Start/pause and reset keys are in Options > Controls > Pegasus Mods. */
@Mod(modid = "ml_stopwatch", name = "Stopwatch", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class StopwatchMod extends SimpleTextHud {
    @Setting(name = "Countdown Timer (seconds, 0 = stopwatch)", min = 0, max = 3600, step = 5)
    public int countdown = 0;
    @Setting(name = "Show Milliseconds")
    public boolean millis = true;

    private final KeyBinding startKey = MLCore.key("Stopwatch: Start / Pause", Keyboard.KEY_NONE);
    private final KeyBinding resetKey = MLCore.key("Stopwatch: Reset", Keyboard.KEY_NONE);

    private long accumulated;
    private long runningSince = -1;

    public StopwatchMod() {
        super("stopwatch", "Stopwatch", "Adds stopwatches and timers to the HUD.", 1f, 0.18f);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (startKey.isPressed()) {
            if (runningSince < 0) runningSince = System.currentTimeMillis();
            else {
                accumulated += System.currentTimeMillis() - runningSince;
                runningSince = -1;
            }
        }
        if (resetKey.isPressed()) {
            accumulated = 0;
            runningSince = -1;
        }
    }

    private long elapsed() {
        return accumulated + (runningSince < 0 ? 0 : System.currentTimeMillis() - runningSince);
    }

    @Override
    protected String text() {
        long ms = elapsed();
        if (countdown > 0) ms = Math.max(0, countdown * 1000L - ms);
        long m = ms / 60000, s = (ms / 1000) % 60, cs = (ms / 10) % 100;
        String t = millis ? String.format("%02d:%02d.%02d", m, s, cs) : String.format("%02d:%02d", m, s);
        return (runningSince < 0 && elapsed() > 0 ? "§7" : "") + t;
    }

    @Override
    protected String sample() {
        return "01:23.45";
    }
}
