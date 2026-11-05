package dev.mylauncher.mods.hotbarkeys;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "ml_hotbarkeys", name = "Hotbar Key Overlay", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HotbarKeysMod extends Feature {
    @Setting(name = "Text Color", color = true)
    public int color = 0xFFFFFFFF;
    @Setting(name = "Text Scale", min = 0.4, max = 1.0, step = 0.05)
    public float textScale = 0.6f;

    public HotbarKeysMod() {
        super("hotbarkeys", "Hotbar Key Overlay", "Show hotbar slot keybinds in the hotbar", Category.HUD);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        int left = event.resolution.getScaledWidth() / 2 - 91;
        int top = event.resolution.getScaledHeight() - 22;
        GlStateManager.disableDepth();
        for (int i = 0; i < 9; i++) {
            int code = mc.gameSettings.keyBindsHotbar[i].getKeyCode();
            String name = code < 0 ? "M" + (code + 101) : Keyboard.getKeyName(code);
            if (name == null) continue;
            if (name.length() > 3) name = name.substring(0, 3);
            GlStateManager.pushMatrix();
            GlStateManager.translate(left + i * 20 + 3, top + 3, 300);
            GlStateManager.scale(textScale, textScale, 1);
            mc.fontRendererObj.drawString(name, 0, 0, color, true);
            GlStateManager.popMatrix();
        }
        GlStateManager.enableDepth();
    }
}
