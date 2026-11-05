package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {
    /** Line background; keeps vanilla fade by reusing the alpha computed by vanilla. */
    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V", ordinal = 0))
    private void ml$lineBackground(int left, int top, int right, int bottom, int color) {
        int custom = Hooks.chatBackground;
        if (custom == Integer.MIN_VALUE) {
            Gui.drawRect(left, top, right, bottom, color);
            return;
        }
        int vanillaAlpha = (color >>> 24) & 0xFF;
        int customAlpha = (custom >>> 24) & 0xFF;
        int alpha = vanillaAlpha * customAlpha / 127;
        if (alpha <= 0) return;
        Gui.drawRect(left, top, right, bottom, (Math.min(255, alpha) << 24) | (custom & 0xFFFFFF));
    }

    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int ml$history(int vanilla) {
        return Hooks.chatHistory;
    }
}
