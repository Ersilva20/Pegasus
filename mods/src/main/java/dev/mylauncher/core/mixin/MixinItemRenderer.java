package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
    }

    private float ml$realSwing;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void ml$capture(float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        ml$realSwing = mc.thePlayer != null ? mc.thePlayer.getSwingProgress(partialTicks) : 0;
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V"))
    private void ml$transform(ItemRenderer self, float equip, float swing) {
        for (Hooks.FirstPersonTransform h : Hooks.firstPerson) {
            swing = h.swing(itemToRender, equip, swing, ml$realSwing);
        }
        for (Hooks.FirstPersonTransform h : Hooks.firstPerson) {
            h.beforeTransform(itemToRender, equip, swing);
        }
        transformFirstPersonItem(equip, swing);
    }
}
