package dev.mylauncher.devtest;

import dev.mylauncher.core.ClientSettings;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.gui.ModMenuScreen;
import dev.mylauncher.core.gui.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


/**
 * Dev-only visual test (never packaged): with -Dpegasus.devtest=true it opens a flat creative world,
 * walks through UI screens, capes and cape-physics motions, saves screenshots to run/screenshots and quits.
 */
@Mod(modid = "pegasus_devtest", name = "Pegasus Dev Test", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class DevTestMod {
    // name, cape, screen ("" / MENU / SETTINGS / CAPE), motion ("" / walk / jump / turn), camera (1 back, 2 front), pack
    private static final String[][] STEPS = {
            {"01-menu", "pegasus", "MENU", "", "1", ""},
            {"01c-menu-grid", "pegasus", "MENU:GRID", "", "1", ""},
            {"01d-menu-list", "pegasus", "MENU:LIST", "", "1", ""},
            {"01e-menu-grid-hud", "pegasus", "MENU:GRID:HUD", "", "1", ""},
            {"01b-physics", "pegasus", "PHYSICS", "", "1", ""},
            {"02-settings", "pegasus", "SETTINGS", "", "1", ""},
            {"03-wardrobe", "file:lunar/aurora.png", "CAPE", "", "1", "ALL"},
            {"04-cape-walk", "file:lunar/akatsuki.png", "", "walk", "1", ""},
            {"05-cape-turn", "file:mojang/2016.png", "", "turn", "1", ""},
            {"06-cape-still", "pegasus", "", "", "1", ""},
    };

    private int ticks;
    private int step = -1;
    private int stepTicks;
    private boolean worldStarted;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        if (Boolean.getBoolean("pegasus.devtest")) MinecraftForge.EVENT_BUS.register(this);
    }

    private static Feature feature(String id) {
        for (Feature f : MLCore.features()) if (f.id.equals(id)) return f;
        return null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!worldStarted && mc.currentScreen instanceof GuiMainMenu && ++ticks > 40) {
            worldStarted = true;
            WorldSettings ws = new WorldSettings(1L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT);
            mc.launchIntegratedServer("pegasus_devtest", "pegasus_devtest", ws);
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (step < 0) {
            if (++stepTicks < 100) return;
            checkMouseKeybinds(mc);
            mc.thePlayer.capabilities.isFlying = false;
            mc.thePlayer.rotationPitch = 0;
            next(mc);
            return;
        }
        String motion = STEPS[step][3];
        double yawRad = Math.toRadians(mc.thePlayer.rotationYaw);
        if ("walk".equals(motion) || "jump".equals(motion)) {
            mc.thePlayer.motionX = -Math.sin(yawRad) * 0.28;
            mc.thePlayer.motionZ = Math.cos(yawRad) * 0.28;
            mc.thePlayer.setSprinting(true);
        }
        if ("jump".equals(motion) && mc.thePlayer.onGround && stepTicks > 20) mc.thePlayer.motionY = 0.42;
        if ("turn".equals(motion)) mc.thePlayer.rotationYaw += 9f;
        if (++stepTicks == 58) {
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, "pegasus-" + STEPS[step][0] + ".png",
                    mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        }
        if (stepTicks >= 62) {
            if (step + 1 >= STEPS.length) {
                mc.shutdown();
                return;
            }
            next(mc);
        }
    }

    /** Simulates mouse side-button clicks (no keyboard event) the way Minecraft's mouse loop does. */
    private void checkMouseKeybinds(Minecraft mc) {
        KeyBinding perspective = mc.gameSettings.keyBindTogglePerspective;
        KeyBinding physicsKey = null;
        for (KeyBinding k : mc.gameSettings.keyBindings) if ("Toggle Cape Physics".equals(k.getKeyDescription())) physicsKey = k;
        int oldPerspective = perspective.getKeyCode(), oldPhysics = physicsKey.getKeyCode();
        perspective.setKeyCode(-97); // mouse button 3 ("button 4")
        physicsKey.setKeyCode(-96);
        KeyBinding.resetKeyBindingArrayAndHash();
        mc.gameSettings.thirdPersonView = 0;
        KeyBinding.onTick(-97);
        FMLCommonHandler.instance().fireMouseInput();
        MLCore.LOG.info("DEVTEST mouse perspective: {}", mc.gameSettings.thirdPersonView == 1 ? "OK" : "FAIL (view " + mc.gameSettings.thirdPersonView + ")");
        Feature physics = feature("physics");
        boolean before = physics.isEnabled();
        KeyBinding.onTick(-96);
        FMLCommonHandler.instance().fireMouseInput();
        MLCore.LOG.info("DEVTEST mouse pegasus keybind: {}", physics.isEnabled() != before ? "OK" : "FAIL");
        if (physics.isEnabled() != before) physics.toggle();
        perspective.setKeyCode(oldPerspective);
        physicsKey.setKeyCode(oldPhysics);
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    private void next(Minecraft mc) {
        step++;
        stepTicks = 0;
        String[] s = STEPS[step];
        mc.thePlayer.setSprinting(false);
        try {
            MLCore.cosmetics.equip(s[1]);
            GuiScreen screen = null;
            if (s[2].startsWith("MENU")) {
                String[] parts = s[2].split(":");
                MLCore.settings.menuLayout = ClientSettings.MenuLayout.valueOf(parts.length > 1 ? parts[1] : "CARDS");
                screen = new ModMenuScreen();
                java.lang.reflect.Field cat = ModMenuScreen.class.getDeclaredField("category");
                cat.setAccessible(true);
                cat.set(screen, parts.length > 2 ? dev.mylauncher.core.Category.valueOf(parts[2]) : null);
            }
            else if ("SETTINGS".equals(s[2])) screen = new SettingsScreen(new ModMenuScreen(), MLCore.settings);
            else if ("PHYSICS".equals(s[2])) screen = new SettingsScreen(new ModMenuScreen(), feature("physics"));
            else if (!s[2].isEmpty()) {
                screen = MLCore.cosmetics.customScreen(null);
                java.lang.reflect.Field pack = screen.getClass().getDeclaredField("pack");
                pack.setAccessible(true);
                pack.set(screen, s[5]);
            }
            mc.gameSettings.hideGUI = screen == null;
            mc.gameSettings.thirdPersonView = screen == null ? Integer.parseInt(s[4]) : 0;
            mc.displayGuiScreen(screen);
        } catch (Exception ex) {
            MLCore.LOG.error("Dev test step failed", ex);
        }
    }
}
