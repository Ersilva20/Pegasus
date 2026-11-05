package dev.mylauncher.mods.packdisplay;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod(modid = "ml_packdisplay", name = "Pack Display", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PackDisplayMod extends HudFeature {
    @Setting(name = "Show Icon")
    public boolean icon = true;
    @Setting(name = "Show All Enabled Packs")
    public boolean all = false;
    @Setting(name = "Background")
    public boolean background = true;
    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0x6F000000;

    public PackDisplayMod() {
        super("packdisplay", "Pack Display", "Shows the currently active Resource Pack on the HUD.", Category.HUD, false, 0f, 0.9f);
    }

    private List<ResourcePackRepository.Entry> packs() {
        return mc.getResourcePackRepository().getRepositoryEntries();
    }

    private int rows() {
        if (!all) return 1;
        return Math.max(1, packs().size());
    }

    @Override
    public int width() {
        int w = mc.fontRendererObj.getStringWidth("Default");
        for (ResourcePackRepository.Entry e : packs()) {
            w = Math.max(w, mc.fontRendererObj.getStringWidth(EnumChatFormatting.getTextWithoutFormattingCodes(e.getResourcePackName())));
        }
        return w + (icon ? 24 : 6);
    }

    @Override
    public int height() {
        return rows() * (icon ? 20 : 12);
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        if (background) Gui.drawRect(0, 0, width(), height(), backgroundColor);
        List<ResourcePackRepository.Entry> list = packs();
        int rowH = icon ? 20 : 12;
        if (list.isEmpty()) {
            mc.fontRendererObj.drawString("Default", icon ? 22 : 3, (rowH - 8) / 2, 0xFFFFFFFF, true);
            return;
        }
        // repositoryEntries is ordered bottom -> top; the last one is the highest priority pack.
        int count = all ? list.size() : 1;
        for (int i = 0; i < count; i++) {
            ResourcePackRepository.Entry e = list.get(list.size() - 1 - i);
            int y = i * rowH;
            if (icon) {
                e.bindTexturePackIcon(mc.getTextureManager());
                GlStateManager.color(1, 1, 1, 1);
                Gui.drawModalRectWithCustomSizedTexture(2, y + 2, 0, 0, 16, 16, 16, 16);
            }
            String name = EnumChatFormatting.getTextWithoutFormattingCodes(e.getResourcePackName()).replace(".zip", "");
            mc.fontRendererObj.drawString(name, icon ? 22 : 3, y + (rowH - 8) / 2, 0xFFFFFFFF, true);
        }
    }
}
