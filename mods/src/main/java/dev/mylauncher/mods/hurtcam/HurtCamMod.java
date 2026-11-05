package dev.mylauncher.mods.hurtcam;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Mod;

/** Scales (or removes) the camera shake when you take damage. 0% = no shake, 100% = vanilla. */
@Mod(modid = "ml_hurtcam", name = "Damage Shake", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HurtCamMod extends Feature implements Hooks.PartialTickCancel {
    @Setting(name = "Shake Intensity (%)", min = 0, max = 200, step = 5)
    public int intensity = 0;
    @Setting(name = "Keep Death Tilt")
    public boolean deathTilt = true;

    public HurtCamMod() {
        super("hurtcam", "Damage Shake", "Lower or turn off the camera shake when you take damage.", Category.PVP);
    }

    @Override
    protected void onEnable() {
        Hooks.hurtCam = this;
    }

    @Override
    protected void onDisable() {
        if (Hooks.hurtCam == this) Hooks.hurtCam = null;
    }

    @Override
    public boolean cancel(float partialTicks) {
        if (!(mc.getRenderViewEntity() instanceof EntityLivingBase)) return true;
        EntityLivingBase e = (EntityLivingBase) mc.getRenderViewEntity();
        if (deathTilt && e.getHealth() <= 0.0F) {
            float d = e.deathTime + partialTicks;
            GlStateManager.rotate(40.0F - 8000.0F / (d + 200.0F), 0.0F, 0.0F, 1.0F);
        }
        float f = e.hurtTime - partialTicks;
        if (f < 0.0F || intensity == 0 || e.maxHurtTime == 0) return true;
        f /= e.maxHurtTime;
        f = MathHelper.sin(f * f * f * f * (float) Math.PI);
        float yaw = e.attackedAtYaw;
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-f * 14.0F * intensity / 100f, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        return true;
    }
}
