package dev.mylauncher.mods.containeranimations;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Containers pop open with a short scale animation. */
@Mod(modid = "ml_containeranimations", name = "Container Animations", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ContainerAnimationsMod extends Feature {
    public enum Type { SCALE, SLIDE_UP, SLIDE_DOWN }

    @Setting(name = "Animation")
    public Type type = Type.SCALE;
    @Setting(name = "Duration (ms)", min = 50, max = 600, step = 25)
    public int duration = 150;

    private long openedAt;
    private boolean pushed;

    public ContainerAnimationsMod() {
        super("containeranimations", "Container Animations", "Animate container screens.", Category.INVENTORY);
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiContainer && !(mc.currentScreen instanceof GuiContainer)) openedAt = System.currentTimeMillis();
    }

    private float progress() {
        float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) duration);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    @SubscribeEvent
    public void onPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer)) return;
        float p = progress();
        if (p >= 1f) return;
        GlStateManager.pushMatrix();
        float cx = event.gui.width / 2f, cy = event.gui.height / 2f;
        switch (type) {
            case SCALE:
                float s = 0.8f + 0.2f * p;
                GlStateManager.translate(cx, cy, 0);
                GlStateManager.scale(s, s, 1);
                GlStateManager.translate(-cx, -cy, 0);
                break;
            case SLIDE_UP:
                GlStateManager.translate(0, (1 - p) * 40, 0);
                break;
            case SLIDE_DOWN:
                GlStateManager.translate(0, -(1 - p) * 40, 0);
                break;
        }
        pushed = true;
    }

    @SubscribeEvent
    public void onPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (pushed) {
            GlStateManager.popMatrix();
            pushed = false;
        }
    }
}
