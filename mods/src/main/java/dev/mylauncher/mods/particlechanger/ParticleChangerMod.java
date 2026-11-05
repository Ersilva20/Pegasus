package dev.mylauncher.mods.particlechanger;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Extra hit particles: multiply crits, always show sharpness sparks, etc. Purely visual. */
@Mod(modid = "ml_particlechanger", name = "Particle Changer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ParticleChangerMod extends Feature {
    @Setting(name = "Multiplier", min = 1, max = 10, step = 1)
    public int multiplier = 2;
    @Setting(name = "Always Show Critical Particles")
    public boolean alwaysCrit = false;
    @Setting(name = "Always Show Sharpness Particles")
    public boolean alwaysSharp = true;
    @Setting(name = "Only On Living Entities")
    public boolean onlyLiving = true;

    public ParticleChangerMod() {
        super("particlechanger", "Particle Changer", "Change how particles look and work.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        if (onlyLiving && !(event.target instanceof EntityLivingBase)) return;
        boolean crit = mc.thePlayer.fallDistance > 0 && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder() && !mc.thePlayer.isInWater();
        boolean sharp = event.target instanceof EntityLivingBase
                && EnchantmentHelper.getModifierForCreature(mc.thePlayer.getHeldItem(), ((EntityLivingBase) event.target).getCreatureAttribute()) > 0;
        // Vanilla already spawns one set; add (multiplier - 1) more, or one set when forced.
        int extraCrit = (crit ? multiplier - 1 : 0) + (alwaysCrit && !crit ? multiplier : 0);
        int extraSharp = (sharp ? multiplier - 1 : 0) + (alwaysSharp && !sharp ? multiplier : 0);
        for (int i = 0; i < extraCrit; i++) mc.effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.CRIT);
        for (int i = 0; i < extraSharp; i++) mc.effectRenderer.emitParticleAtEntity(event.target, EnumParticleTypes.CRIT_MAGIC);
    }
}
