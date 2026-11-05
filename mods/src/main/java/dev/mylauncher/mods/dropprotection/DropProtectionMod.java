package dev.mylauncher.mods.dropprotection;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/** Blocks dropping (Q / Ctrl+Q / dragging out) of the item types you list, or of any enchanted/named item. */
@Mod(modid = "ml_dropprotection", name = "Drop Guard", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class DropProtectionMod extends Feature implements Hooks.DropItem, Hooks.SlotClick {
    @Setting(name = "Protected Items (comma separated ids)")
    public String items = "diamond_sword, diamond_pickaxe, bow, golden_apple, ender_pearl";
    @Setting(name = "Protect Enchanted Items")
    public boolean enchanted = true;
    @Setting(name = "Protect Renamed Items")
    public boolean named = true;
    @Setting(name = "Show Warning")
    public boolean warn = true;

    private Set<Item> protectedItems = new HashSet<Item>();
    private long lastWarn;

    public DropProtectionMod() {
        super("dropprotection", "Drop Guard", "Stops chosen items from being dropped by accident.", Category.INVENTORY);
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        Set<Item> out = new HashSet<Item>();
        for (String s : items.split(",")) {
            String id = s.trim();
            if (id.isEmpty()) continue;
            Item i = Item.getByNameOrId(id.contains(":") ? id : "minecraft:" + id);
            if (i != null) out.add(i);
        }
        protectedItems = out;
    }

    @Override
    protected void onEnable() {
        Hooks.dropItem.add(this);
        Hooks.slotClick.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.dropItem.remove(this);
        Hooks.slotClick.remove(this);
    }

    private boolean isProtected(ItemStack s) {
        if (s == null) return false;
        if (protectedItems.contains(s.getItem())) return true;
        if (enchanted && s.isItemEnchanted()) return true;
        return named && s.hasDisplayName();
    }

    private boolean block() {
        if (warn && System.currentTimeMillis() - lastWarn > 1500) {
            lastWarn = System.currentTimeMillis();
            MLCore.chat("That item is drop-protected. Turn off Drop Guard to drop it.");
        }
        return true;
    }

    @Override
    public boolean onDrop(ItemStack held, boolean fullStack) {
        return isProtected(held) && block();
    }

    @Override
    public boolean onClick(GuiContainer gui, Slot slot, int slotId, int button, int clickType) {
        // clickType 4 = Q over a slot; slotId -999 = clicking outside the window with an item on the cursor.
        if (clickType == 4 && slot != null && isProtected(slot.getStack())) return block();
        if (slotId == -999 && isProtected(mc.thePlayer.inventory.getItemStack())) return block();
        return false;
    }
}
