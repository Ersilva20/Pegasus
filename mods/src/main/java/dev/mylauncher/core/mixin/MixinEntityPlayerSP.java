package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {
    @Inject(method = "dropOneItem", at = @At("HEAD"), cancellable = true)
    private void ml$drop(boolean fullStack, CallbackInfoReturnable<EntityItem> cir) {
        EntityPlayerSP self = (EntityPlayerSP) (Object) this;
        for (Hooks.DropItem h : Hooks.dropItem) {
            if (h.onDrop(self.getCurrentEquippedItem(), fullStack)) {
                cir.setReturnValue(null);
                return;
            }
        }
    }
}
