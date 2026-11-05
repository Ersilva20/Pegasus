package dev.mylauncher.core.mixin;

import dev.mylauncher.core.MLCore;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.stats.Achievement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Settings > Show Achievements. */
@Mixin(GuiAchievement.class)
public class MixinGuiAchievement {
    @Inject(method = "displayAchievement", at = @At("HEAD"), cancellable = true)
    private void ml$achievement(Achievement ach, CallbackInfo ci) {
        if (MLCore.settings != null && !MLCore.settings.showAchievements) ci.cancel();
    }

    @Inject(method = "displayUnformattedAchievement", at = @At("HEAD"), cancellable = true)
    private void ml$hint(Achievement ach, CallbackInfo ci) {
        if (MLCore.settings != null && !MLCore.settings.showAchievements) ci.cancel();
    }
}
