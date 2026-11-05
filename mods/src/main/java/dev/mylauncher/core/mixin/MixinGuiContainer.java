package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    private void ml$slotClick(Slot slot, int slotId, int button, int clickType, CallbackInfo ci) {
        for (Hooks.SlotClick h : Hooks.slotClick) {
            if (h.onClick((GuiContainer) (Object) this, slot, slotId, button, clickType)) {
                ci.cancel();
                return;
            }
        }
    }
}
