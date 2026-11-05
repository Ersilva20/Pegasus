package dev.mylauncher.mods.slotbinding;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.mixin.GuiContainerAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Bind an inventory slot to a hotbar slot: hover an inventory slot, press the bind key, then hover a
 * hotbar slot and press it again. Afterwards shift-clicking the bound slot swaps it with its hotbar slot.
 */
@Mod(modid = "ml_slotbinding", name = "Slot Swap", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class SlotBindingMod extends Feature implements Hooks.SlotClick {
    public static class Data {
        public Map<Integer, Integer> bindings = new HashMap<Integer, Integer>();
    }

    @Setting(name = "Line Color", color = true)
    public int color = 0xFF55FFFF;

    private final KeyBinding key = MLCore.key("Bind Slot", Keyboard.KEY_K);
    private Data data;
    private int pending = -1;

    public SlotBindingMod() {
        super("slotbinding", "Slot Swap", "Link an inventory slot to a hotbar slot, then shift-click to swap them.", Category.INVENTORY);
    }

    private Data data() {
        if (data == null) data = Config.readJson("slotbinding_data.json", Data.class, new Data());
        return data;
    }

    @Override
    protected void onEnable() {
        Hooks.slotClick.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.slotClick.remove(this);
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiInventory) || !Keyboard.getEventKeyState() || Keyboard.getEventKey() != key.getKeyCode()) return;
        Slot s = ((GuiContainerAccessor) event.gui).ml$getHoveredSlot();
        if (s == null || s.inventory != mc.thePlayer.inventory) return;
        int idx = s.getSlotIndex();
        if (pending < 0) {
            if (idx < 9) {
                if (data().bindings.values().remove(idx)) MLCore.chat("Removed binding for hotbar slot " + (idx + 1));
                Config.writeJson("slotbinding_data.json", data());
                return;
            }
            pending = idx;
            MLCore.chat("Now hover a hotbar slot and press the bind key again.");
        } else {
            if (idx < 9) {
                data().bindings.put(pending, idx);
                Config.writeJson("slotbinding_data.json", data());
                MLCore.chat("Bound inventory slot to hotbar slot " + (idx + 1));
            }
            pending = -1;
        }
    }

    @Override
    public boolean onClick(GuiContainer gui, Slot slot, int slotId, int button, int clickType) {
        if (!(gui instanceof GuiInventory) || clickType != 1 || slot == null || slot.inventory != mc.thePlayer.inventory) return false;
        Integer hotbar = data().bindings.get(slot.getSlotIndex());
        if (hotbar == null) return false;
        mc.playerController.windowClick(gui.inventorySlots.windowId, slotId, hotbar, 2, mc.thePlayer);
        return true;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiInventory) || data().bindings.isEmpty()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        GuiContainerAccessor a = (GuiContainerAccessor) gui;
        Slot hovered = a.ml$getHoveredSlot();
        if (hovered == null || hovered.inventory != mc.thePlayer.inventory) return;
        int idx = hovered.getSlotIndex();
        for (Map.Entry<Integer, Integer> e : data().bindings.entrySet()) {
            if (e.getKey() != idx && e.getValue() != idx) continue;
            Slot from = find(gui, e.getKey()), to = find(gui, e.getValue());
            if (from == null || to == null) continue;
            GlStateManager.disableDepth();
            int x1 = a.ml$getGuiLeft() + from.xDisplayPosition, y1 = a.ml$getGuiTop() + from.yDisplayPosition;
            int x2 = a.ml$getGuiLeft() + to.xDisplayPosition, y2 = a.ml$getGuiTop() + to.yDisplayPosition;
            dev.mylauncher.core.util.RenderUtil.outline(x1 - 1, y1 - 1, x1 + 17, y1 + 17, color);
            dev.mylauncher.core.util.RenderUtil.outline(x2 - 1, y2 - 1, x2 + 17, y2 + 17, color);
            Gui.drawRect(Math.min(x1, x2) + 8, y1 + 8, Math.max(x1, x2) + 9, y1 + 9, color);
            Gui.drawRect(x2 + 8, Math.min(y1, y2) + 8, x2 + 9, Math.max(y1, y2) + 9, color);
            GlStateManager.enableDepth();
        }
    }

    private Slot find(GuiContainer gui, int index) {
        for (Slot s : gui.inventorySlots.inventorySlots) if (s.inventory == mc.thePlayer.inventory && s.getSlotIndex() == index) return s;
        return null;
    }
}
