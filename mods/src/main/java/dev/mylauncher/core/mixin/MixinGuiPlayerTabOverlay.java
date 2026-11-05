package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {
    @Inject(method = "renderPlayerlist", at = @At("HEAD"))
    private void ml$scaleStart(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo ci) {
        float s = Hooks.tabScale;
        GlStateManager.pushMatrix();
        if (s != 1.0f) {
            GlStateManager.translate(width / 2.0f, 0, 0);
            GlStateManager.scale(s, s, 1);
            GlStateManager.translate(-width / 2.0f, 0, 0);
        }
    }

    @Inject(method = "renderPlayerlist", at = @At("RETURN"))
    private void ml$scaleEnd(int width, Scoreboard scoreboard, ScoreObjective objective, CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    @Inject(method = "drawPing", at = @At("HEAD"), cancellable = true)
    private void ml$ping(int width, int x, int y, NetworkPlayerInfo info, CallbackInfo ci) {
        if (Hooks.tabPing != null && Hooks.tabPing.render(width, x, y, info)) ci.cancel();
    }
}
