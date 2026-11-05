package dev.mylauncher.mods.fov;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.init.Items;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Control the dynamic FOV from sprinting, speed/slowness potions, flying and bows. */
@Mod(modid = "ml_fov", name = "FOV Control", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class FovMod extends Feature {
    @Setting(name = "Sprinting Multiplier", min = 1, max = 1.5, step = 0.01)
    public float sprint = 1.15f;
    @Setting(name = "Speed Potion Effect")
    public boolean speed = false;
    @Setting(name = "Slowness Potion Effect")
    public boolean slowness = false;
    @Setting(name = "Flying Effect")
    public boolean flying = true;
    @Setting(name = "Bow Zoom")
    public boolean bow = true;

    public FovMod() {
        super("fov", "FOV Control", "Control how sprinting, potions, flying and bows change your field of view.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        float f = 1.0f;
        if (flying && event.entity.capabilities.isFlying) f *= 1.1f;
        if (event.entity.isSprinting()) f *= sprint;
        if (speed && event.entity.isPotionActive(Potion.moveSpeed)) {
            f *= 1.0f + 0.1f * (event.entity.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        if (slowness && event.entity.isPotionActive(Potion.moveSlowdown)) {
            f *= 1.0f - 0.075f * (event.entity.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() + 1);
        }
        if (bow && event.entity.isUsingItem() && event.entity.getItemInUse().getItem() == Items.bow) {
            float t = event.entity.getItemInUseDuration() / 20.0f;
            t = t > 1.0f ? 1.0f : t * t;
            f *= 1.0f - t * 0.15f;
        }
        event.newfov = f;
    }
}
