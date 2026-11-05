package dev.mylauncher.mods.inventoryhud;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_inventoryhud", name = "Inventory Hud", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class InventoryHudMod extends HudFeature {
    @Setting(name = "Background")
    public boolean background = true;
    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0x6F000000;
    @Setting(name = "Show Hotbar Row")
    public boolean hotbar = false;
    @Setting(name = "Hide When Empty")
    public boolean hideEmpty = true;

    public InventoryHudMod() {
        super("inventoryhud", "Inventory Hud", "A hud mod that shows your inventory contents.", Category.HUD, false, 0.5f, 0.75f);
    }

    private int rows() {
        return hotbar ? 4 : 3;
    }

    @Override
    public boolean shouldRender() {
        if (!hideEmpty) return true;
        for (int i = 9; i < 36; i++) if (mc.thePlayer.inventory.mainInventory[i] != null) return true;
        return false;
    }

    @Override
    public int width() {
        return 9 * 18 + 2;
    }

    @Override
    public int height() {
        return rows() * 18 + 2;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        if (background) Gui.drawRect(0, 0, width(), height(), backgroundColor);
        if (!inGame()) return;
        ItemStack[] inv = mc.thePlayer.inventory.mainInventory;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                RenderUtil.item(inv[9 + row * 9 + col], 1 + col * 18, 1 + row * 18, true);
            }
        }
        if (hotbar) {
            for (int col = 0; col < 9; col++) RenderUtil.item(inv[col], 1 + col * 18, 1 + 3 * 18, true);
        }
    }
}
