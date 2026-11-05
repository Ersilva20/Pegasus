package dev.mylauncher.mods.reachdisplay;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_reachdisplay", name = "Reach Display", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ReachDisplayMod extends SimpleTextHud {
    @Setting(name = "Reset After (seconds)", min = 1, max = 10, step = 1)
    public int resetSeconds = 3;
    @Setting(name = "Decimals", min = 1, max = 3, step = 1)
    public int decimals = 2;

    private double lastReach = -1;
    private long lastHit;

    public ReachDisplayMod() {
        super("reachdisplay", "Reach Display", "Shows the distance of your last hit on a player or mob.", 0f, 0.24f);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer != mc.thePlayer) return;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || mop.hitVec == null) return;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        lastReach = eyes.distanceTo(mop.hitVec);
        lastHit = System.currentTimeMillis();
    }

    @Override
    protected String text() {
        if (lastReach < 0 || System.currentTimeMillis() - lastHit > resetSeconds * 1000L) return "Hasn't attacked";
        return String.format("%." + decimals + "f blocks", lastReach);
    }

    @Override
    protected String sample() {
        return "3.00 blocks";
    }
}
