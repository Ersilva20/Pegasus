package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge {
    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void ml$title(int width, int height, float partialTicks, CallbackInfo ci) {
        if (Hooks.title != null && Hooks.title.render((GuiIngame) (Object) this, width, height, partialTicks)) ci.cancel();
    }

    @Inject(method = "renderRecordOverlay", at = @At("HEAD"), cancellable = true)
    private void ml$actionBar(int width, int height, float partialTicks, CallbackInfo ci) {
        if (Hooks.actionBar != null && Hooks.actionBar.render((GuiIngame) (Object) this, width, height, partialTicks)) ci.cancel();
    }
}
