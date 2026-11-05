package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {
    /** Mouse look calls setAngles on the camera entity; Freelook/Snaplook redirect it to a free camera. */
    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void ml$turn(float yaw, float pitch, CallbackInfo ci) {
        if (Hooks.turn != null && Hooks.turn.onTurn((Entity) (Object) this, yaw, pitch)) ci.cancel();
    }
}
