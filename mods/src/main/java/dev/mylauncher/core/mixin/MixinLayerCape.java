package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the Physics mod draw capes as simulated cloth instead of vanilla's rigid slab. */
@Mixin(LayerCape.class)
public class MixinLayerCape {
    @Inject(method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void ml$capeRender(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks,
                               float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (Hooks.capeRenderer != null && Hooks.capeRenderer.render(player, partialTicks)) ci.cancel();
    }
}
