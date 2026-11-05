package dev.mylauncher.mods.combo;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Counts hits that landed on the target (it took damage) without you getting hit in between. */
@Mod(modid = "ml_combo", name = "Combo Counter", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ComboMod extends SimpleTextHud {
    @Setting(name = "Expire After (seconds)", min = 1, max = 5, step = 0.5)
    public double expire = 2;

    private int combo;
    private long lastHitTime;
    private Entity pendingTarget;
    private long pendingTime;
    private int lastPlayerHurt;

    public ComboMod() {
        super("combo", "Combo Counter", "Displays how often you have hit another player within a certain amount of time on the HUD.", 0f, 0.36f);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        pendingTarget = event.target;
        pendingTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !inGame()) return;
        long now = System.currentTimeMillis();
        if (pendingTarget instanceof EntityLivingBase) {
            EntityLivingBase t = (EntityLivingBase) pendingTarget;
            if (t.hurtTime == t.maxHurtTime && t.maxHurtTime > 0 && now - pendingTime < 400) {
                combo++;
                lastHitTime = now;
                pendingTarget = null;
            } else if (now - pendingTime >= 400) {
                pendingTarget = null;
            }
        }
        int hurt = mc.thePlayer.hurtTime;
        if (hurt > 0 && lastPlayerHurt == 0) combo = 0;
        lastPlayerHurt = hurt;
        if (combo > 0 && now - lastHitTime > expire * 1000) combo = 0;
    }

    @Override
    protected String text() {
        return combo == 0 ? "No Combo" : combo + " Combo";
    }

    @Override
    protected String sample() {
        return "3 Combo";
    }
}
