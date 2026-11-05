package dev.mylauncher.core.mixin;

import dev.mylauncher.core.ClientSettings;
import dev.mylauncher.core.MLCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Settings tab: Hit Delay Fix, FPS limits and Borderless Fullscreen. */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow private int leftClickCounter;
    @Shadow private boolean fullscreen;
    @Shadow private int tempDisplayWidth;
    @Shadow private int tempDisplayHeight;
    @Shadow public int displayWidth;
    @Shadow public int displayHeight;
    @Shadow public GameSettings gameSettings;
    @Shadow public WorldClient theWorld;
    @Shadow public GuiScreen currentScreen;

    @Shadow private void resize(int width, int height) {}
    @Shadow private void updateFramebufferSize() {}
    @Shadow public abstract void updateDisplay();

    @Unique private boolean ml$borderless;

    /** Hit Delay Fix (same as the HitDelayFix mod): no 10-tick cooldown after a missed swing. */
    @Inject(method = "clickMouse", at = @At("RETURN"))
    private void ml$hitDelayFix(CallbackInfo ci) {
        ClientSettings s = MLCore.settings;
        if (s != null && s.hitDelayFix) leftClickCounter = 0;
    }

    @Inject(method = "getLimitFramerate", at = @At("HEAD"), cancellable = true)
    private void ml$fpsLimits(CallbackInfoReturnable<Integer> cir) {
        ClientSettings s = MLCore.settings;
        if (s == null) return;
        if (!Display.isActive()) cir.setReturnValue(Math.min(s.unfocusedFps, gameSettings.limitFramerate));
        else if (theWorld == null && currentScreen != null) cir.setReturnValue(s.menuFps);
    }

    /** Borderless Fullscreen: F11 turns the window into an undecorated, screen-sized window instead. */
    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
    private void ml$borderless(CallbackInfo ci) {
        ClientSettings s = MLCore.settings;
        boolean wanted = s != null && s.borderlessFullscreen;
        if (!wanted && !ml$borderless) return; // vanilla fullscreen
        ci.cancel();
        try {
            ml$borderless = !ml$borderless;
            fullscreen = ml$borderless; // vanilla must not treat the window as resizable while borderless
            System.setProperty("org.lwjgl.opengl.Window.undecorated", String.valueOf(ml$borderless));
            Display.setFullscreen(false);
            if (ml$borderless) {
                DisplayMode desktop = Display.getDesktopDisplayMode();
                Display.setDisplayMode(new DisplayMode(desktop.getWidth(), desktop.getHeight()));
                Display.setLocation(0, 0);
                Display.setResizable(false);
            } else {
                Display.setDisplayMode(new DisplayMode(tempDisplayWidth, tempDisplayHeight));
                Display.setResizable(true);
                Display.setLocation(-1, -1); // centered
            }
            displayWidth = Math.max(1, Display.getDisplayMode().getWidth());
            displayHeight = Math.max(1, Display.getDisplayMode().getHeight());
            if (currentScreen != null) resize(displayWidth, displayHeight);
            else updateFramebufferSize();
            Display.setVSyncEnabled(gameSettings.enableVsync);
            updateDisplay();
        } catch (Exception e) {
            MLCore.LOG.error("Borderless fullscreen failed", e);
        }
    }
}
