package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The second GlStateManager.color call in renderGlint sets the purple armor glint tint. */
@Mixin(LayerArmorBase.class)
public class MixinLayerArmorBase {
    @Redirect(method = "renderGlint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V", ordinal = 1))
    private void ml$armorGlint(float r, float g, float b, float a) {
        float[] c = Hooks.armorGlint;
        if (c != null) GlStateManager.color(c[0], c[1], c[2], a);
        else GlStateManager.color(r, g, b, a);
    }
}
