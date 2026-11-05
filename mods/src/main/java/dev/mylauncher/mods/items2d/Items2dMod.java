package dev.mylauncher.mods.items2d;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Mod;

/** Dropped items always face the camera like flat sprites instead of spinning. */
@Mod(modid = "ml_items2d", name = "2D Items", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class Items2dMod extends Feature implements Hooks.DroppedItemTransform {
    @Setting(name = "Keep Bobbing")
    public boolean bob = true;

    public Items2dMod() {
        super("items2d", "2D Items", "Render dropped blocks and items in a 2D perspective, always facing you.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        Hooks.droppedItemTransform.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.droppedItemTransform.remove(this);
    }

    @Override
    public Integer transform(EntityItem item, double x, double y, double z, float pt, IBakedModel model) {
        ItemStack stack = item.getEntityItem();
        if (stack == null || stack.getItem() == null) return null;
        float bobY = bob ? MathHelper.sin((item.getAge() + pt) / 10.0F + item.hoverStart) * 0.1F + 0.1F : 0.05f;
        GlStateManager.translate((float) x, (float) y + bobY + 0.25f, (float) z);
        GlStateManager.rotate(180f - mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
        return 1;
    }
}
