package dev.mylauncher.mods.uhcoverlay;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

/** Makes gold, golden apples and heads bigger on the ground so they're easy to spot in UHC. */
@Mod(modid = "ml_uhcoverlay", name = "UHC Overlay", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class UhcOverlayMod extends Feature implements Hooks.DroppedItemScale {
    @Setting(name = "Gold Ingot Size", min = 1, max = 5, step = 0.25)
    public float ingot = 2f;
    @Setting(name = "Gold Nugget Size", min = 1, max = 5, step = 0.25)
    public float nugget = 2f;
    @Setting(name = "Gold Ore Size", min = 1, max = 5, step = 0.25)
    public float ore = 2f;
    @Setting(name = "Golden Apple Size", min = 1, max = 5, step = 0.25)
    public float apple = 2.5f;
    @Setting(name = "Head / Skull Size", min = 1, max = 5, step = 0.25)
    public float head = 2.5f;

    public UhcOverlayMod() {
        super("uhcoverlay", "UHC Overlay", "Resize items that are commonly needed in UHC to make spotting them easier.", Category.PVP);
    }

    @Override
    protected void onEnable() {
        Hooks.droppedItemScale.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.droppedItemScale.remove(this);
    }

    @Override
    public float scale(EntityItem entity) {
        ItemStack s = entity.getEntityItem();
        if (s == null) return 1f;
        Item i = s.getItem();
        if (i == Items.gold_ingot) return ingot;
        if (i == Items.gold_nugget) return nugget;
        if (i == Item.getItemFromBlock(Blocks.gold_ore)) return ore;
        if (i == Items.golden_apple) return apple;
        if (i == Items.skull) return head;
        return 1f;
    }
}
