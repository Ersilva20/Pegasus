package dev.mylauncher.mods.itemcounter;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Type item ids separated by commas, e.g. "golden_apple, ender_pearl, arrow". */
@Mod(modid = "ml_itemcounter", name = "Item Counter", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ItemCounterMod extends HudFeature {
    @Setting(name = "Items (comma separated ids)")
    public String items = "golden_apple, ender_pearl, arrow";
    @Setting(name = "Horizontal")
    public boolean horizontal = false;
    @Setting(name = "Hide When Zero")
    public boolean hideZero = false;
    @Setting(name = "Background")
    public boolean background = true;
    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0x6F000000;

    private List<Item> parsed = new ArrayList<Item>();

    public ItemCounterMod() {
        super("itemcounter", "Item Counter", "Counts how many of the item in your hand you carry in total.",
                Category.HUD, false, 1f, 0.5f);
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        List<Item> out = new ArrayList<Item>();
        for (String s : items.split(",")) {
            String id = s.trim();
            if (id.isEmpty()) continue;
            Item item = Item.getByNameOrId(id.contains(":") ? id : "minecraft:" + id);
            if (item != null) out.add(item);
        }
        parsed = out;
    }

    private int count(Item item) {
        if (!inGame()) return 0;
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) if (s != null && s.getItem() == item) n += s.stackSize;
        return n;
    }

    private List<Item> shown(boolean editing) {
        if (editing || !hideZero) return parsed;
        List<Item> out = new ArrayList<Item>();
        for (Item i : parsed) if (count(i) > 0) out.add(i);
        return out;
    }

    @Override
    public boolean shouldRender() {
        return !shown(false).isEmpty();
    }

    @Override
    public int width() {
        int n = Math.max(1, shown(true).size());
        return horizontal ? n * 36 + 2 : 38;
    }

    @Override
    public int height() {
        int n = Math.max(1, shown(true).size());
        return horizontal ? 20 : n * 18 + 2;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        if (background) Gui.drawRect(0, 0, width(), height(), backgroundColor);
        int i = 0;
        for (Item item : shown(editing)) {
            int x = horizontal ? 2 + i * 36 : 2;
            int y = horizontal ? 2 : 2 + i * 18;
            RenderUtil.item(new ItemStack(item), x, y, false);
            mc.fontRendererObj.drawString(String.valueOf(editing && !inGame() ? 16 : count(item)), x + 18, y + 4, 0xFFFFFFFF, true);
            i++;
        }
    }
}
