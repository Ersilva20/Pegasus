package dev.mylauncher.mods.oldvisuals;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.7-style animations: swinging while blocking/eating/drinking/using a bow ("block-hit"),
 * and the slightly lower/rotated 1.7 sword block position.
 */
@Mod(modid = "ml_oldvisuals", name = "1.7 Visuals", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class OldVisualsMod extends Feature implements Hooks.FirstPersonTransform {
    @Setting(name = "Block-Hit Animation")
    public boolean blockHit = true;
    @Setting(name = "Swing While Eating/Drinking")
    public boolean eatSwing = true;
    @Setting(name = "Swing While Using Bow")
    public boolean bowSwing = true;
    @Setting(name = "1.7 Sword Block Position")
    public boolean oldBlock = true;

    public OldVisualsMod() {
        super("oldvisuals", "1.7 Visuals", "Revert certain visuals and animations to how they behaved on 1.7.", Category.PVP);
    }

    @Override
    protected void onEnable() {
        Hooks.firstPerson.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.firstPerson.remove(this);
    }

    private EnumAction action(ItemStack stack) {
        if (stack == null || mc.thePlayer == null || mc.thePlayer.getItemInUseCount() <= 0) return null;
        return stack.getItemUseAction();
    }

    @Override
    public float swing(ItemStack stack, float equipProgress, float vanillaSwing, float realSwing) {
        EnumAction a = action(stack);
        if (a == null) return vanillaSwing;
        switch (a) {
            case BLOCK:
                return blockHit ? realSwing : vanillaSwing;
            case EAT:
            case DRINK:
                return eatSwing ? realSwing : vanillaSwing;
            case BOW:
                return bowSwing ? realSwing : vanillaSwing;
            default:
                return vanillaSwing;
        }
    }

    @Override
    public void beforeTransform(ItemStack stack, float equipProgress, float swingProgress) {
        if (oldBlock && action(stack) == EnumAction.BLOCK && stack.getItem() instanceof ItemSword) {
            GlStateManager.translate(-0.05f, 0.08f, 0.02f);
        }
    }
}
