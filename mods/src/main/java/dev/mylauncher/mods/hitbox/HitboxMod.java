package dev.mylauncher.mods.hitbox;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

/** Customizes the F3+B hitboxes (press F3+B to show them, as in vanilla). */
@Mod(modid = "ml_hitbox", name = "Hitbox", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HitboxMod extends Feature implements Hooks.HitboxRenderer {
    @Setting(name = "Player Color", color = true)
    public int playerColor = 0xFFFFFFFF;
    @Setting(name = "Mob Color", color = true)
    public int mobColor = 0xFFFFFF55;
    @Setting(name = "Item Color", color = true)
    public int itemColor = 0xFF55FF55;
    @Setting(name = "Projectile Color", color = true)
    public int projectileColor = 0xFFFF55FF;
    @Setting(name = "Show Players")
    public boolean players = true;
    @Setting(name = "Show Mobs")
    public boolean mobs = true;
    @Setting(name = "Show Items")
    public boolean items = true;
    @Setting(name = "Show Projectiles")
    public boolean projectiles = true;
    @Setting(name = "Show Eye Height")
    public boolean eyeLine = true;
    @Setting(name = "Show Look Vector")
    public boolean lookVector = true;
    @Setting(name = "Line Width", min = 1, max = 5, step = 0.5)
    public float width = 2f;

    public HitboxMod() {
        super("hitbox", "Hitbox", "Change the look of the F3+B hitboxes.", Category.PVP);
    }

    @Override
    protected void onEnable() {
        Hooks.hitbox = this;
    }

    @Override
    protected void onDisable() {
        if (Hooks.hitbox == this) Hooks.hitbox = null;
    }

    @Override
    public boolean render(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        int color;
        if (entity instanceof EntityPlayer) {
            if (!players) return true;
            color = playerColor;
        } else if (entity instanceof EntityItem) {
            if (!items) return true;
            color = itemColor;
        } else if (entity instanceof EntityArrow || entity instanceof EntityThrowable) {
            if (!projectiles) return true;
            color = projectileColor;
        } else {
            if (!mobs) return true;
            color = mobColor;
        }
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GL11.glLineWidth(width);
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        AxisAlignedBB box = new AxisAlignedBB(bb.minX - entity.posX + x, bb.minY - entity.posY + y, bb.minZ - entity.posZ + z,
                bb.maxX - entity.posX + x, bb.maxY - entity.posY + y, bb.maxZ - entity.posZ + z);
        RenderUtil.boxOutline(box, color);
        if (entity instanceof EntityLivingBase) {
            if (eyeLine) {
                double eye = entity.getEyeHeight();
                RenderUtil.boxOutline(new AxisAlignedBB(box.minX, y + eye - 0.01, box.minZ, box.maxX, y + eye + 0.01, box.maxZ), 0xFFFF0000);
            }
            if (lookVector) {
                Vec3 look = entity.getLook(partialTicks);
                double eye = entity.getEyeHeight();
                RenderUtil.line(x, y + eye, z, x + look.xCoord * 2, y + eye + look.yCoord * 2, z + look.zCoord * 2, 0xFF5555FF);
            }
        }
        GL11.glLineWidth(1f);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(1, 1, 1, 1);
        return true;
    }
}
