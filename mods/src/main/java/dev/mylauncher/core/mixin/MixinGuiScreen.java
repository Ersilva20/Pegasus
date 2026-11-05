package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge's 4-argument drawHoveringText is used for item tooltips; offset it for Scrollable Tooltips. */
@Mixin(GuiScreen.class)
public class MixinGuiScreen {
    @ModifyVariable(method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private int ml$tooltipX(int x) {
        return x + Hooks.tooltipOffsetX;
    }

    @ModifyVariable(method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private int ml$tooltipY(int y) {
        return y + Hooks.tooltipOffsetY;
    }

    /** Same side button fix as MixinMinecraft, for clicks inside screens. */
    @Redirect(method = "handleInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false))
    private boolean ml$mouseNext() {
        return dev.mylauncher.core.util.MouseFix.next();
    }
}
