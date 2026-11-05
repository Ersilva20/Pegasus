package dev.mylauncher.mods.freelook;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Hold Left Alt to look around without turning your player. The camera angles are swapped onto the
 * player only while a frame is drawn, so the server keeps seeing your real rotation.
 */
@Mod(modid = "ml_freelook", name = "Freelook", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class FreelookMod extends Feature implements Hooks.AnglesHook {
    @Setting(name = "Toggle Instead of Hold")
    public boolean toggleMode = false;
    @Setting(name = "Switch to Third Person")
    public boolean thirdPerson = true;
    @Setting(name = "Invert Pitch")
    public boolean invertPitch = false;

    private final KeyBinding key = MLCore.key("Freelook", Keyboard.KEY_LMENU);
    private boolean active;
    private float camYaw, camPitch;
    private int savedPerspective;
    private float realYaw, realPitch, realPrevYaw, realPrevPitch;
    private boolean swapped;

    public FreelookMod() {
        super("freelook", "Freelook", "Turn the camera around your player while you keep walking straight.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        Hooks.turn = this;
    }

    @Override
    protected void onDisable() {
        if (active) stop();
        if (Hooks.turn == this) Hooks.turn = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        boolean want;
        if (toggleMode) {
            want = active;
            if (key.isPressed()) want = !active;
        } else {
            want = key.isKeyDown() && mc.currentScreen == null;
        }
        if (want && !active) start();
        else if (!want && active) stop();
    }

    private void start() {
        active = true;
        camYaw = mc.thePlayer.rotationYaw;
        camPitch = mc.thePlayer.rotationPitch;
        savedPerspective = mc.gameSettings.thirdPersonView;
        if (thirdPerson) mc.gameSettings.thirdPersonView = 1;
    }

    private void stop() {
        active = false;
        mc.gameSettings.thirdPersonView = savedPerspective;
    }

    @Override
    public boolean onTurn(Entity entity, float yaw, float pitch) {
        if (!active || entity != mc.thePlayer) return false;
        camYaw += yaw * 0.15f;
        camPitch += (invertPitch ? pitch : -pitch) * 0.15f;
        camPitch = Math.max(-90f, Math.min(90f, camPitch));
        return true;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (mc.thePlayer == null) return;
        if (event.phase == TickEvent.Phase.START && active) {
            realYaw = mc.thePlayer.rotationYaw;
            realPitch = mc.thePlayer.rotationPitch;
            realPrevYaw = mc.thePlayer.prevRotationYaw;
            realPrevPitch = mc.thePlayer.prevRotationPitch;
            mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = camYaw;
            mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = camPitch;
            swapped = true;
        } else if (event.phase == TickEvent.Phase.END && swapped) {
            mc.thePlayer.rotationYaw = realYaw;
            mc.thePlayer.rotationPitch = realPitch;
            mc.thePlayer.prevRotationYaw = realPrevYaw;
            mc.thePlayer.prevRotationPitch = realPrevPitch;
            swapped = false;
        }
    }
}
