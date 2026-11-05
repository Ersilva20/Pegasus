package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.renderer.entity.RenderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public class MixinRenderItem {
    @ModifyConstant(method = "renderEffect", constant = @Constant(intValue = -8372020))
    private int ml$glintColor(int vanilla) {
        return Hooks.itemGlint;
    }

    @Inject(method = "renderItemAndEffectIntoGUI", at = @At("RETURN"))
    private void ml$afterGuiItem(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack == null) return;
        for (Hooks.GuiItemOverlay h : Hooks.guiItem) h.afterItem(stack, x, y);
    }
}
