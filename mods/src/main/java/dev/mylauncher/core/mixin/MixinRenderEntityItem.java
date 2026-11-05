package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** func_177077_a positions/rotates a dropped item and returns how many copies to draw. */
@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem {
    private static void ml$scale(EntityItem item) {
        float s = 1.0f;
        for (Hooks.DroppedItemScale h : Hooks.droppedItemScale) s *= h.scale(item);
        if (s != 1.0f) GlStateManager.scale(s, s, s);
    }

    @Inject(method = "func_177077_a", at = @At("HEAD"), cancellable = true)
    private void ml$transform(EntityItem item, double x, double y, double z, float pt, IBakedModel model,
                              CallbackInfoReturnable<Integer> cir) {
        for (Hooks.DroppedItemTransform h : Hooks.droppedItemTransform) {
            Integer count = h.transform(item, x, y, z, pt, model);
            if (count != null) {
                ml$scale(item);
                cir.setReturnValue(count);
                return;
            }
        }
    }

    @Inject(method = "func_177077_a", at = @At("RETURN"))
    private void ml$afterTransform(EntityItem item, double x, double y, double z, float pt, IBakedModel model,
                                   CallbackInfoReturnable<Integer> cir) {
        ml$scale(item);
    }
}
