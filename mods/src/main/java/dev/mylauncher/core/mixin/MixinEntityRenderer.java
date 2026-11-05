package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Shadow private void setupViewBobbing(float partialTicks) {}

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void ml$hurtCam(float partialTicks, CallbackInfo ci) {
        if (Hooks.hurtCam != null && Hooks.hurtCam.cancel(partialTicks)) ci.cancel();
    }

    /** Settings > Minimal View Bobbing: skip the camera bob; the hand still bobs (renderHand calls it separately). */
    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V"))
    private void ml$minimalBobbing(EntityRenderer self, float partialTicks) {
        if (MLCore.settings == null || !MLCore.settings.minimalViewBobbing) setupViewBobbing(partialTicks);
    }
}
