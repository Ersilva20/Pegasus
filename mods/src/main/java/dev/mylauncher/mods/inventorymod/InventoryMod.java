package dev.mylauncher.mods.inventorymod;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.mixin.GuiContainerAccessor;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Inventory quality-of-life: no potion shift and optionally no potion list beside the inventory. */
@Mod(modid = "ml_inventorymod", name = "Inventory Extras", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class InventoryMod extends Feature {
    @Setting(name = "No Potion Shift", description = "Keep the inventory centered when you have effects")
    public boolean noShift = true;
    @Setting(name = "Hide Potion Effects in Inventory")
    public boolean hideEffects = false;

    public InventoryMod() {
        super("inventorymod", "Inventory Extras", "Stops the inventory moving sideways when you have potion effects.", Category.INVENTORY);
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof InventoryEffectRenderer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        GuiContainerAccessor a = (GuiContainerAccessor) gui;
        if (noShift) a.ml$setGuiLeft((gui.width - a.ml$getXSize()) / 2);
        if (hideEffects) {
            try {
                ReflectionHelper.setPrivateValue(InventoryEffectRenderer.class, (InventoryEffectRenderer) gui, false,
                        "hasActivePotionEffects", "field_147045_u");
            } catch (Exception ignored) {
            }
        }
    }
}
