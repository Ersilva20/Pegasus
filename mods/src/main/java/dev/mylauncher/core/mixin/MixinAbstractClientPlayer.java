package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {
    @Inject(method = "getLocationSkin()Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void ml$skin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (Hooks.skin == null) return;
        ResourceLocation r = Hooks.skin.skin((AbstractClientPlayer) (Object) this);
        if (r != null) cir.setReturnValue(r);
    }

    @Inject(method = "getLocationCape()Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void ml$cape(CallbackInfoReturnable<ResourceLocation> cir) {
        if (Hooks.cape == null) return;
        ResourceLocation r = Hooks.cape.skin((AbstractClientPlayer) (Object) this);
        if (r != null) cir.setReturnValue(r);
    }
}
