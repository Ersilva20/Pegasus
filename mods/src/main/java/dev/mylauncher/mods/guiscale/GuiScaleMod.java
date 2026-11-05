package dev.mylauncher.mods.guiscale;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Separate GUI scale for menus/inventories and for the in-game HUD (0 = Auto, 1-4). */
@Mod(modid = "ml_guiscale", name = "GUI Scale", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class GuiScaleMod extends Feature {
    @Setting(name = "HUD Scale (0 = Auto)", min = 0, max = 4, step = 1)
    public int hudScale = 2;
    @Setting(name = "Menu Scale (0 = Auto)", min = 0, max = 4, step = 1)
    public int menuScale = 3;
    @Setting(name = "Only Inventories Use Menu Scale")
    public boolean onlyContainers = false;

    private int original = -1;

    public GuiScaleMod() {
        super("guiscale", "GUI Scale", "Allows you to set a custom GUI scale different from the Minecraft settings.", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        original = mc.gameSettings.guiScale;
        mc.gameSettings.guiScale = hudScale;
    }

    @Override
    protected void onDisable() {
        if (original >= 0) mc.gameSettings.guiScale = original;
    }

    @Override
    public void onSettingsChanged() {
        if (isEnabled() && mc.currentScreen == null) mc.gameSettings.guiScale = hudScale;
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        if (event.gui == null) {
            mc.gameSettings.guiScale = hudScale;
        } else if (!(event.gui instanceof GuiChat) && (!onlyContainers || event.gui instanceof GuiContainer)) {
            mc.gameSettings.guiScale = menuScale;
        }
    }
}
