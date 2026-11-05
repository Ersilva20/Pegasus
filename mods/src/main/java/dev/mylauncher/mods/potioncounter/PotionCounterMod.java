package dev.mylauncher.mods.potioncounter;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod(modid = "ml_potioncounter", name = "Potion Counter", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PotionCounterMod extends SimpleTextHud {
    public enum Mode { POTIONS, SOUPS, BOTH }

    @Setting(name = "Count")
    public Mode mode = Mode.POTIONS;
    @Setting(name = "Only Splash Potions")
    public boolean splashOnly = true;

    public PotionCounterMod() {
        super("potioncounter", "Potion Counter", "Counts the healing potions and soups left in your inventory.", 0f, 0.48f);
    }

    private boolean isHealPotion(ItemStack s) {
        if (!(s.getItem() instanceof ItemPotion)) return false;
        if (splashOnly && !ItemPotion.isSplash(s.getMetadata())) return false;
        List<PotionEffect> effects = ((ItemPotion) s.getItem()).getEffects(s);
        if (effects == null) return false;
        for (PotionEffect e : effects) if (e.getPotionID() == Potion.heal.id) return true;
        return false;
    }

    @Override
    protected String text() {
        int pots = 0, soups = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s == null) continue;
            if (isHealPotion(s)) pots += s.stackSize;
            if (s.getItem() == Items.mushroom_stew) soups += s.stackSize;
        }
        switch (mode) {
            case SOUPS:
                return soups + " Soups";
            case BOTH:
                return pots + " Pots | " + soups + " Soups";
            default:
                return pots + " Pots";
        }
    }

    @Override
    protected String sample() {
        return "18 Pots";
    }
}
