package dev.mylauncher.mods.itemcustomizer;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

/** First-person held item position/size and dropped item size. */
@Mod(modid = "ml_itemcustomizer", name = "Held Item Tweaks", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ItemCustomizerMod extends Feature implements Hooks.FirstPersonTransform, Hooks.DroppedItemScale {
    @Setting(name = "Held Item X Offset", min = -1, max = 1, step = 0.02)
    public float x = 0f;
    @Setting(name = "Held Item Y Offset", min = -1, max = 1, step = 0.02)
    public float y = 0f;
    @Setting(name = "Held Item Z Offset", min = -1, max = 1, step = 0.02)
    public float z = 0f;
    @Setting(name = "Held Item Size", min = 0.1, max = 2, step = 0.05)
    public float heldScale = 1f;
    @Setting(name = "Swing Speed Animation Off", description = "Hides the swing animation (only visually)")
    public boolean noSwing = false;
    @Setting(name = "Dropped Item Size", min = 0.25, max = 4, step = 0.05)
    public float droppedScale = 1f;

    public ItemCustomizerMod() {
        super("itemcustomizer", "Held Item Tweaks", "Change the size and position of held and dropped items.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        Hooks.firstPerson.add(this);
        Hooks.droppedItemScale.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.firstPerson.remove(this);
        Hooks.droppedItemScale.remove(this);
    }

    @Override
    public void beforeTransform(ItemStack stack, float equipProgress, float swingProgress) {
        GlStateManager.translate(x, y, z);
        if (heldScale != 1f) GlStateManager.scale(heldScale, heldScale, heldScale);
    }

    @Override
    public float swing(ItemStack stack, float equipProgress, float vanillaSwing, float realSwing) {
        return noSwing ? 0f : vanillaSwing;
    }

    @Override
    public float scale(EntityItem item) {
        return droppedScale;
    }
}
