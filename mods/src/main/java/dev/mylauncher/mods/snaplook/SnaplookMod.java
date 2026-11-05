package dev.mylauncher.mods.snaplook;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Hold the key to switch perspective, release to go back. */
@Mod(modid = "ml_snaplook", name = "Snaplook", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class SnaplookMod extends Feature {
    public enum View { BACK, FRONT }

    @Setting(name = "Camera")
    public View view = View.FRONT;

    private final KeyBinding key = MLCore.key("Snaplook", org.lwjgl.input.Keyboard.KEY_NONE);
    private boolean active;
    private int saved;

    public SnaplookMod() {
        super("snaplook", "Snaplook", "When activated, allows you to change your camera perspective while a button is pressed.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        boolean down = key.isKeyDown() && mc.currentScreen == null;
        if (down && !active) {
            active = true;
            saved = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = view == View.FRONT ? 2 : 1;
        } else if (!down && active) {
            active = false;
            mc.gameSettings.thirdPersonView = saved;
        }
    }
}
