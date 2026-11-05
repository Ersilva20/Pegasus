package dev.mylauncher.mods.hotbarcustomizer;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Move the hotbar anywhere (drag it in the HUD editor) and choose horizontal or vertical layout. */
@Mod(modid = "ml_hotbarcustomizer", name = "Hotbar Layout", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HotbarCustomizerMod extends HudFeature {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final Gui GUI = new Gui();

    public enum Style { VANILLA, MINIMAL }

    @Setting(name = "Vertical")
    public boolean vertical = false;
    @Setting(name = "Style")
    public Style style = Style.VANILLA;
    @Setting(name = "Minimal Background", color = true)
    public int minimalBackground = 0x70000000;
    @Setting(name = "Minimal Selected", color = true)
    public int minimalSelected = 0xA0FFFFFF;

    public HotbarCustomizerMod() {
        super("hotbarcustomizer", "Hotbar Layout", "Move the hotbar anywhere and change its shape.", Category.HUD, false, 0.5f, 1f);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) event.setCanceled(true);
    }

    @Override
    public int width() {
        return vertical ? 22 : 182;
    }

    @Override
    public int height() {
        return vertical ? 182 : 22;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        int selected = inGame() ? mc.thePlayer.inventory.currentItem : 0;
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        if (style == Style.VANILLA) {
            mc.getTextureManager().bindTexture(WIDGETS);
            for (int i = 0; i < 9; i++) {
                int x = vertical ? 0 : i * 20, y = vertical ? i * 20 : 0;
                int u = i == 0 ? 0 : 1 + i * 20;
                int w = i == 0 ? 21 : i == 8 ? 21 : 20;
                GUI.drawTexturedModalRect(x + (i == 0 ? 0 : 1), y + (vertical && i > 0 ? 1 : 0), u, 0, w, 22);
            }
            int sx = vertical ? -1 : selected * 20 - 1, sy = vertical ? selected * 20 - 1 : -1;
            GUI.drawTexturedModalRect(sx, sy, 0, 22, 24, 24);
        } else {
            for (int i = 0; i < 9; i++) {
                int x = vertical ? 0 : i * 20, y = vertical ? i * 20 : 0;
                Gui.drawRect(x + 1, y + 1, x + 21, y + 21, i == selected ? minimalSelected : minimalBackground);
            }
        }
        if (!inGame()) return;
        for (int i = 0; i < 9; i++) {
            int x = vertical ? 3 : i * 20 + 3, y = vertical ? i * 20 + 3 : 3;
            RenderUtil.item(mc.thePlayer.inventory.mainInventory[i], x, y, true);
        }
    }
}
