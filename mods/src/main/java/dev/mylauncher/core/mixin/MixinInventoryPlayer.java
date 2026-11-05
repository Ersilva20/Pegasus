package dev.mylauncher.core.mixin;

import dev.mylauncher.core.MLCore;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Settings > Disable Hotbar Scrolling (changeCurrentItem is only called by the mouse wheel). */
@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer {
    @Inject(method = "changeCurrentItem", at = @At("HEAD"), cancellable = true)
    private void ml$noScroll(int direction, CallbackInfo ci) {
        if (MLCore.settings != null && MLCore.settings.disableHotbarScrolling) ci.cancel();
    }
}
