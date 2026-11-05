package dev.mylauncher.mods.bettersounds;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Change the volume of any sound. Rules: "sound.prefix=percent", comma separated, e.g.
 * "random.explode=30, mob.zombie=0, dig=80". The first matching prefix wins.
 */
@Mod(modid = "ml_bettersounds", name = "Sound Mixer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class BetterSoundsMod extends Feature {
    @Setting(name = "Rules (sound=percent, ...)")
    public String rules = "random.explode=50";
    @Setting(name = "Rain Volume (%)", min = 0, max = 100, step = 5)
    public int rain = 100;
    @Setting(name = "Ender Pearl / Teleport Volume (%)", min = 0, max = 200, step = 5)
    public int teleport = 100;
    @Setting(name = "Note Block Volume (%)", min = 0, max = 200, step = 5)
    public int noteBlocks = 100;

    private final Map<String, Float> parsed = new LinkedHashMap<String, Float>();

    public BetterSoundsMod() {
        super("bettersounds", "Sound Mixer", "Set the volume of each sound separately.", Category.UTILITY);
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        parsed.clear();
        parsed.put("ambient.weather.rain", rain / 100f);
        parsed.put("mob.endermen.portal", teleport / 100f);
        parsed.put("note.", noteBlocks / 100f);
        for (String part : rules.split(",")) {
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            try {
                parsed.put(kv[0].trim(), Float.parseFloat(kv[1].trim()) / 100f);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onSound(PlaySoundEvent event) {
        if (event.result == null || event.result instanceof ITickableSound) return;
        String name = event.name;
        for (Map.Entry<String, Float> e : parsed.entrySet()) {
            if (!name.startsWith(e.getKey())) continue;
            float f = e.getValue();
            if (f == 1f) return;
            event.result = f <= 0f ? null : new Scaled(event.result, f);
            return;
        }
    }

    /** Wraps a sound and scales its volume. */
    private static final class Scaled implements ISound {
        private final ISound s;
        private final float factor;

        Scaled(ISound s, float factor) {
            this.s = s;
            this.factor = factor;
        }

        @Override
        public ResourceLocation getSoundLocation() {
            return s.getSoundLocation();
        }

        @Override
        public boolean canRepeat() {
            return s.canRepeat();
        }

        @Override
        public int getRepeatDelay() {
            return s.getRepeatDelay();
        }

        @Override
        public float getVolume() {
            return s.getVolume() * factor;
        }

        @Override
        public float getPitch() {
            return s.getPitch();
        }

        @Override
        public float getXPosF() {
            return s.getXPosF();
        }

        @Override
        public float getYPosF() {
            return s.getYPosF();
        }

        @Override
        public float getZPosF() {
            return s.getZPosF();
        }

        @Override
        public AttenuationType getAttenuationType() {
            return s.getAttenuationType();
        }
    }
}
