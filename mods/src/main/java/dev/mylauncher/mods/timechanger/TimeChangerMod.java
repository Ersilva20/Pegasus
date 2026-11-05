package dev.mylauncher.mods.timechanger;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Only the drawn sky changes; the real world time is restored right after the world renders. */
@Mod(modid = "ml_timechanger", name = "Time Changer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class TimeChangerMod extends Feature {
    public enum Mode { FIXED, DAY, SUNSET, NIGHT, MIDNIGHT, FAST_CYCLE }

    @Setting(name = "Mode")
    public Mode mode = Mode.DAY;
    @Setting(name = "Fixed Time (ticks)", min = 0, max = 24000, step = 250)
    public int time = 6000;
    @Setting(name = "Cycle Speed", min = 1, max = 100, step = 1)
    public int cycleSpeed = 20;

    private long real;
    private boolean swapped;
    private long cycle;

    public TimeChangerMod() {
        super("timechanger", "Time Changer", "Choose the time of day you see. Only changes your screen.", Category.WORLD);
    }

    private long target() {
        switch (mode) {
            case DAY:
                return 6000;
            case SUNSET:
                return 12500;
            case NIGHT:
                return 15000;
            case MIDNIGHT:
                return 18000;
            case FAST_CYCLE:
                return cycle % 24000;
            default:
                return time;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) cycle += cycleSpeed;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.theWorld == null) return;
        real = mc.theWorld.getWorldTime();
        long day = real - (real % 24000);
        mc.theWorld.setWorldTime(day + target());
        swapped = true;
    }

    @SubscribeEvent
    public void onWorldDone(RenderWorldLastEvent event) {
        restore();
    }

    @SubscribeEvent
    public void onRenderEnd(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) restore();
    }

    private void restore() {
        if (swapped && mc.theWorld != null) {
            mc.theWorld.setWorldTime(real);
            swapped = false;
        }
    }
}
