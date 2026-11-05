package dev.mylauncher.mods.weatherchanger;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Client-side only: changes what you see, not the server's weather. */
@Mod(modid = "ml_weatherchanger", name = "Weather Changer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class WeatherChangerMod extends Feature {
    public enum Weather { CLEAR, RAIN, THUNDER }

    @Setting(name = "Weather")
    public Weather weather = Weather.CLEAR;
    @Setting(name = "Rain Strength", min = 0, max = 1, step = 0.05)
    public float strength = 1f;

    public WeatherChangerMod() {
        super("weatherchanger", "Weather Changer", "Customize the current weather.", Category.WORLD);
    }

    private void apply() {
        if (mc.theWorld == null) return;
        float rain = weather == Weather.CLEAR ? 0f : strength;
        float thunder = weather == Weather.THUNDER ? strength : 0f;
        mc.theWorld.setRainStrength(rain);
        mc.theWorld.setThunderStrength(thunder);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) apply();
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) apply();
    }

    @Override
    protected void onDisable() {
        if (mc.theWorld != null) {
            boolean raining = mc.theWorld.getWorldInfo().isRaining();
            boolean thundering = mc.theWorld.getWorldInfo().isThundering();
            mc.theWorld.setRainStrength(raining ? 1f : 0f);
            mc.theWorld.setThunderStrength(thundering ? 1f : 0f);
        }
    }
}
