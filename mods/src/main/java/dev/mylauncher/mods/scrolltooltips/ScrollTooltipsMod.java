package dev.mylauncher.mods.scrolltooltips;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

/** Scroll the mouse wheel over an item to move its tooltip (hold Shift for sideways). */
@Mod(modid = "ml_scrolltooltips", name = "Scrollable Tooltips", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ScrollTooltipsMod extends Feature {
    @Setting(name = "Scroll Speed", min = 2, max = 30, step = 1)
    public int speed = 10;

    private Slot lastSlot;

    public ScrollTooltipsMod() {
        super("scrolltooltips", "Scrollable Tooltips", "Use your scroll wheel to read overly long item descriptions.", Category.INVENTORY);
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private void reset() {
        Hooks.tooltipOffsetX = 0;
        Hooks.tooltipOffsetY = 0;
    }

    private Slot hovered(GuiContainer gui) {
        try {
            return ReflectionHelper.getPrivateValue(GuiContainer.class, gui, "theSlot", "field_147006_u");
        } catch (Exception e) {
            return null;
        }
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        reset();
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer)) return;
        Slot s = hovered((GuiContainer) event.gui);
        if (s != lastSlot) {
            reset();
            lastSlot = s;
        }
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer)) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        Slot s = hovered((GuiContainer) event.gui);
        if (s == null || !s.getHasStack()) return;
        int step = Integer.signum(wheel) * speed;
        if (GuiScreen.isShiftKeyDown()) Hooks.tooltipOffsetX += step;
        else Hooks.tooltipOffsetY += step;
    }
}
