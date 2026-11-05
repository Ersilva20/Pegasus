package dev.mylauncher.mods.itemphysics;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

/** Dropped items lie flat on the ground and tumble while falling, instead of floating and spinning. */
@Mod(modid = "ml_itemphysics", name = "Item Physics", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ItemPhysicsMod extends Feature implements Hooks.DroppedItemTransform {
    @Setting(name = "Tumble Speed", min = 0, max = 5, step = 0.25)
    public float speed = 1.5f;

    public ItemPhysicsMod() {
        super("itemphysics", "Item Physics", "Replaces ground items with ones that have physics.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        Hooks.droppedItemTransform.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.droppedItemTransform.remove(this);
    }

    static int copies(ItemStack stack) {
        int n = stack.stackSize;
        return n > 48 ? 5 : n > 32 ? 4 : n > 16 ? 3 : n > 1 ? 2 : 1;
    }

    @Override
    public Integer transform(EntityItem item, double x, double y, double z, float pt, IBakedModel model) {
        ItemStack stack = item.getEntityItem();
        if (stack == null || stack.getItem() == null) return null;
        boolean block = model.isGui3d();
        GlStateManager.translate((float) x, (float) y + (block ? 0.12f : 0.02f), (float) z);
        // Stable random yaw per item, derived from its hover start.
        float yaw = item.hoverStart * 57.29578f * 4f;
        GlStateManager.rotate(yaw, 0, 1, 0);
        if (!block) {
            GlStateManager.rotate(90f, 1, 0, 0);
        }
        if (!item.onGround && !item.isInWater()) {
            float spin = (item.getAge() + pt) * 10f * speed;
            GlStateManager.rotate(spin, 1, 0, 0);
        }
        GlStateManager.color(1, 1, 1, 1);
        return copies(stack);
    }
}
