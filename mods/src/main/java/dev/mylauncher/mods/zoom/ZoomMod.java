package dev.mylauncher.mods.zoom;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Hold C (change in Controls) to zoom; scroll while zooming to adjust. */
@Mod(modid = "ml_zoom", name = "Zoom", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ZoomMod extends Feature {
    @Setting(name = "Zoom Amount", min = 2, max = 20, step = 0.5)
    public float zoomAmount = 4f;
    @Setting(name = "Scroll to Adjust")
    public boolean scroll = true;
    @Setting(name = "Smooth Zoom Animation")
    public boolean smooth = true;
    @Setting(name = "Cinematic Camera While Zooming")
    public boolean cinematic = false;
    @Setting(name = "Toggle Instead of Hold")
    public boolean toggleMode = false;
    @Setting(name = "Lower Sensitivity While Zooming")
    public boolean lowerSensitivity = true;

    private final KeyBinding key = MLCore.key("Zoom", Keyboard.KEY_C);
    private boolean zooming;
    private float current = 1f, previous = 1f;
    private float scrollFactor = 1f;
    private boolean savedSmooth;
    private float savedSensitivity = -1;

    public ZoomMod() {
        super("zoom", "Zoom", "Hold a key to zoom in; scroll to zoom further.", Category.VISUAL, true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean want = toggleMode ? (key.isPressed() != zooming) : key.isKeyDown() && mc.currentScreen == null;
        if (want && !zooming) start();
        else if (!want && zooming) stop();
        zooming = want;
        previous = current;
        float target = zooming ? zoomAmount * scrollFactor : 1f;
        current = smooth ? current + (target - current) * 0.5f : target;
    }

    private void start() {
        scrollFactor = 1f;
        if (cinematic) {
            savedSmooth = mc.gameSettings.smoothCamera;
            mc.gameSettings.smoothCamera = true;
        }
        if (lowerSensitivity) {
            savedSensitivity = mc.gameSettings.mouseSensitivity;
        }
    }

    private void stop() {
        if (cinematic) mc.gameSettings.smoothCamera = savedSmooth;
        if (savedSensitivity >= 0) {
            mc.gameSettings.mouseSensitivity = savedSensitivity;
            savedSensitivity = -1;
        }
    }

    @Override
    protected void onDisable() {
        stop();
        zooming = false;
        current = previous = 1f;
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        float pt = (float) event.renderPartialTicks;
        float factor = previous + (current - previous) * pt;
        if (factor > 1.001f) {
            event.setFOV(event.getFOV() / factor);
            if (savedSensitivity >= 0) {
                mc.gameSettings.mouseSensitivity = savedSensitivity * Math.max(0.25f, 1f / (float) Math.sqrt(factor));
            }
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!zooming || !scroll || event.dwheel == 0) return;
        scrollFactor *= event.dwheel > 0 ? 1.25f : 0.8f;
        scrollFactor = Math.max(0.5f, Math.min(10f, scrollFactor));
        event.setCanceled(true);
    }
}
