package dev.mylauncher.mods.pvpinfo;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.TextHud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Melee accuracy, health info and projectile stats for the current session. */
@Mod(modid = "ml_pvpinfo", name = "PvP Info", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PvpInfoMod extends TextHud {
    @Setting(name = "Melee Stats")
    public boolean melee = true;
    @Setting(name = "Health Stats")
    public boolean health = true;
    @Setting(name = "Projectile Stats")
    public boolean projectile = true;

    private int swings, hits, landed;
    private int arrowsShot;
    private EntityLivingBase target;
    private int lastArrowCount = -1;
    private boolean wasUsingBow;

    public PvpInfoMod() {
        super("pvpinfo", "PvP Info", "Displays various PvP statistics", Category.PVP, false, 1f, 0.4f);
        fixedWidth = false;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        swings = hits = landed = arrowsShot = 0;
        target = null;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button == 0 && event.buttonstate && mc.currentScreen == null) swings++;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        hits++;
        if (event.target instanceof EntityLivingBase) target = (EntityLivingBase) event.target;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !inGame()) return;
        if (target != null && target.hurtTime == target.maxHurtTime && target.maxHurtTime > 0) landed++;
        boolean using = mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse() != null && mc.thePlayer.getItemInUse().getItem() == Items.bow;
        if (wasUsingBow && !using) arrowsShot++;
        wasUsingBow = using;
        if (target != null && (target.isDead || target.getDistanceToEntity(mc.thePlayer) > 64)) target = null;
    }

    @Override
    protected List<String> lines() {
        List<String> out = new ArrayList<String>();
        if (melee) {
            int acc = swings == 0 ? 0 : Math.min(100, hits * 100 / swings);
            out.add("Hits: " + landed + " / " + swings + " (" + acc + "%)");
        }
        if (health) {
            out.add(String.format("Health: %.1f", mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()));
            if (target != null) out.add(String.format("Target: %.1f", target.getHealth()));
        }
        if (projectile) {
            int stuck = 0;
            for (Object o : mc.theWorld.loadedEntityList) {
                if (o instanceof EntityArrow && ((EntityArrow) o).shootingEntity == mc.thePlayer) stuck++;
            }
            out.add("Arrows shot: " + arrowsShot + (stuck > 0 ? " (" + stuck + " flying)" : ""));
        }
        return out;
    }

    @Override
    protected List<String> sampleLines() {
        return Arrays.asList("Hits: 42 / 60 (70%)", "Health: 18.0", "Target: 6.5", "Arrows shot: 3");
    }
}
