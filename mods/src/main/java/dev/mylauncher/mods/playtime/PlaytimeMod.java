package dev.mylauncher.mods.playtime;

import dev.mylauncher.core.Config;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Session time and total time played (saved to config/pegasus/playtime_total.json). */
@Mod(modid = "ml_playtime", name = "Playtime", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PlaytimeMod extends SimpleTextHud {
    public enum Mode { SESSION, TOTAL, BOTH }

    public static class Data {
        public long totalSeconds;
    }

    @Setting(name = "Show")
    public Mode mode = Mode.SESSION;

    private final long gameStart = System.currentTimeMillis();
    private Data data;
    private int ticks;

    public PlaytimeMod() {
        super("playtime", "Playtime", "Track how long you are playing.", 1f, 0.12f);
    }

    private Data data() {
        if (data == null) data = Config.readJson("playtime_total.json", Data.class, new Data());
        return data;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !inGame()) return;
        if (++ticks % 20 == 0) {
            data().totalSeconds++;
            if (ticks % 1200 == 0) Config.writeJson("playtime_total.json", data());
        }
    }

    @Override
    protected void onDisable() {
        if (data != null) Config.writeJson("playtime_total.json", data);
    }

    private static String fmt(long seconds) {
        long h = seconds / 3600, m = (seconds / 60) % 60, s = seconds % 60;
        return h > 0 ? String.format("%dh %02dm", h, m) : String.format("%dm %02ds", m, s);
    }

    @Override
    protected String text() {
        String session = fmt((System.currentTimeMillis() - gameStart) / 1000L);
        switch (mode) {
            case TOTAL:
                return "Total: " + fmt(data().totalSeconds);
            case BOTH:
                return session + " | " + fmt(data().totalSeconds);
            default:
                return "Playtime: " + session;
        }
    }
}
