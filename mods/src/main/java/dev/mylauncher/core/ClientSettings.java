package dev.mylauncher.core;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Pegasus' own settings (the SETTINGS tab of the menu), modelled on Lunar's General / Performance settings.
 * Always on. The core mixins read these fields directly (see MixinMinecraft, MixinEntityRenderer...).
 */
public class ClientSettings extends Feature {
    // ---- General
    @Setting(name = "Hit Delay Fix", section = "GENERAL",
            description = "Removes 1.8's half-second delay after missing a hit, so you can attack again right away")
    public boolean hitDelayFix = true;
    @Setting(name = "Minimal View Bobbing", description = "With View Bobbing on, only your hand bobs (the camera stays still)")
    public boolean minimalViewBobbing = false;
    @Setting(name = "Show Achievements", description = "Show the achievement popups in the top right")
    public boolean showAchievements = true;
    @Setting(name = "Disable Hotbar Scrolling", description = "The mouse wheel no longer changes your selected slot")
    public boolean disableHotbarScrolling = false;
    @Setting(name = "Borderless Fullscreen", description = "F11 makes a borderless window instead of exclusive fullscreen (fast alt-tab)")
    public boolean borderlessFullscreen = false;
    @Setting(name = "Mod Menu Layout", description = "Cards (small cards with icons), Grid (3 big tiles with a preview) or List (wide rows, no icons)")
    public MenuLayout menuLayout = MenuLayout.CARDS;

    public enum MenuLayout { CARDS, GRID, LIST }

    // ---- Movement (from the Toggle Sneak mod)
    @Setting(name = "Toggle Sprint", section = "MOVEMENT", description = "Press sprint once to keep sprinting")
    public boolean toggleSprint = true;
    @Setting(name = "Toggle Sneak", description = "Press sneak once to keep sneaking")
    public boolean toggleSneak = false;
    @Setting(name = "Show Sprint/Sneak Status", description = "Shows [Sprinting (Toggled)] on the HUD (move it in Edit HUD)")
    public boolean sprintStatus = true;

    // ---- Performance
    @Setting(name = "Max Unfocused FPS", section = "PERFORMANCE", description = "FPS limit while the game window is not in focus",
            min = 5, max = 240, step = 5)
    public int unfocusedFps = 30;
    @Setting(name = "Max Main Menu FPS", description = "FPS limit in the main menu (vanilla is 30)", min = 30, max = 240, step = 10)
    public int menuFps = 60;
    @Setting(name = "Entity Render Distance", description = "Mobs and items further than this are not drawn (128 = no limit)",
            min = 16, max = 128, step = 8)
    public int entityDistance = 128;
    @Setting(name = "Tile Entity Render Distance", description = "Chests, signs, skulls... further than this are not drawn (64 = vanilla)",
            min = 8, max = 64, step = 4)
    public int tileEntityDistance = 64;

    boolean sprinting;
    boolean sneaking;

    public ClientSettings() {
        super("general", "Settings", "Pegasus settings", Category.UTILITY, true);
    }

    @Override
    public boolean builtIn() {
        return true;
    }

    @Override
    public void onSettingsChanged() {
        if (!toggleSprint) sprinting = false;
        if (!toggleSneak) sneaking = false;
    }

    // ---------------------------------------------------------------- toggle sprint / sneak

    private static boolean physicallyDown(KeyBinding k) {
        int c = k.getKeyCode();
        if (c == 0) return false;
        return c < 0 ? Mouse.isButtonDown(c + 100) : Keyboard.isKeyDown(c);
    }

    private void handleToggle() {
        if (mc.currentScreen != null || mc.gameSettings == null) return;
        if (toggleSprint && mc.gameSettings.keyBindSprint.isPressed()) sprinting = !sprinting;
        if (toggleSneak && mc.gameSettings.keyBindSneak.isPressed()) sneaking = !sneaking;
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        handleToggle();
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        handleToggle();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !inGame()) return;
        KeyBinding sprint = mc.gameSettings.keyBindSprint, sneak = mc.gameSettings.keyBindSneak;
        if (toggleSprint) KeyBinding.setKeyBindState(sprint.getKeyCode(), sprinting || physicallyDown(sprint));
        if (toggleSneak) KeyBinding.setKeyBindState(sneak.getKeyCode(), sneaking || physicallyDown(sneak));
    }
}
