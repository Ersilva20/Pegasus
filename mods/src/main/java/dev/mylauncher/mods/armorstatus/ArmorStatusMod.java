package dev.mylauncher.mods.armorstatus;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = "ml_armorstatus", name = "Armor Status", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ArmorStatusMod extends HudFeature {
    public enum Durability { VALUE, PERCENT, VALUE_MAX, NONE }

    @Setting(name = "Show Held Item")
    public boolean heldItem = true;
    @Setting(name = "Horizontal")
    public boolean horizontal = false;
    @Setting(name = "Durability Text")
    public Durability durability = Durability.VALUE;
    @Setting(name = "Color by Durability")
    public boolean colored = true;
    @Setting(name = "Text on Left")
    public boolean textLeft = false;

    public ArmorStatusMod() {
        super("armorstatus", "Armor Status", "Shows your armor pieces and held item with their durability.",
                Category.HUD, false, 1f, 0.75f);
    }

    private List<ItemStack> stacks(boolean editing) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (inGame()) {
            for (int i = 3; i >= 0; i--) {
                ItemStack s = mc.thePlayer.inventory.armorInventory[i];
                if (s != null) out.add(s);
            }
            if (heldItem && mc.thePlayer.getHeldItem() != null) out.add(mc.thePlayer.getHeldItem());
        }
        if (out.isEmpty() && editing) {
            out.add(new ItemStack(Items.diamond_helmet));
            out.add(new ItemStack(Items.diamond_chestplate));
            out.add(new ItemStack(Items.diamond_leggings));
            out.add(new ItemStack(Items.diamond_boots));
            out.add(new ItemStack(Items.diamond_sword));
        }
        return out;
    }

    private String text(ItemStack s) {
        if (!s.isItemStackDamageable()) {
            if (s.stackSize > 1) return String.valueOf(countInInventory(s));
            return "";
        }
        int left = s.getMaxDamage() - s.getItemDamage();
        switch (durability) {
            case PERCENT:
                return (left * 100 / s.getMaxDamage()) + "%";
            case VALUE_MAX:
                return left + "/" + s.getMaxDamage();
            case NONE:
                return "";
            default:
                return String.valueOf(left);
        }
    }

    private int countInInventory(ItemStack held) {
        if (!inGame()) return held.stackSize;
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null && s.isItemEqual(held)) n += s.stackSize;
        }
        return n;
    }

    private int color(ItemStack s) {
        if (!colored || !s.isItemStackDamageable()) return 0xFFFFFFFF;
        float pct = (s.getMaxDamage() - s.getItemDamage()) / (float) s.getMaxDamage();
        return pct > 0.6f ? 0xFF55FF55 : pct > 0.3f ? 0xFFFFFF55 : pct > 0.15f ? 0xFFFFAA00 : 0xFFFF5555;
    }

    private int textWidth(boolean editing) {
        int w = 0;
        for (ItemStack s : stacks(editing)) w = Math.max(w, mc.fontRendererObj.getStringWidth(text(s)));
        return w;
    }

    @Override
    public boolean shouldRender() {
        return !stacks(false).isEmpty();
    }

    @Override
    public int width() {
        int n = Math.max(1, stacks(true).size());
        return horizontal ? n * (20 + textWidth(true)) : 20 + textWidth(true);
    }

    @Override
    public int height() {
        return horizontal ? 18 : Math.max(1, stacks(true).size()) * 18;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        int tw = textWidth(editing);
        int i = 0;
        for (ItemStack s : stacks(editing)) {
            int x = horizontal ? i * (20 + tw) : 0;
            int y = horizontal ? 0 : i * 18;
            String t = text(s);
            if (textLeft && !horizontal) {
                mc.fontRendererObj.drawString(t, x + tw - mc.fontRendererObj.getStringWidth(t), y + 5, color(s), true);
                RenderUtil.item(s, x + tw + 2, y + 1, false);
            } else {
                RenderUtil.item(s, x, y + 1, false);
                mc.fontRendererObj.drawString(t, x + 18, y + 5, color(s), true);
            }
            i++;
        }
    }
}
