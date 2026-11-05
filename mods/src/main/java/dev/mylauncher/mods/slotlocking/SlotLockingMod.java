package dev.mylauncher.mods.slotlocking;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.mixin.GuiContainerAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.HashSet;
import java.util.Set;

/**
 * Hover a slot of your inventory and press L to lock it. Locked slots can't be clicked, shift-clicked,
 * swapped or dropped (Q), so you can't lose that item by accident.
 */
@Mod(modid = "ml_slotlocking", name = "Slot Locking", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class SlotLockingMod extends Feature implements Hooks.SlotClick, Hooks.DropItem {
    public static class Data {
        public Set<Integer> locked = new HashSet<Integer>();
    }

    @Setting(name = "Lock Overlay Color", color = true)
    public int color = 0x60FF3333;

    private final KeyBinding key = MLCore.key("Lock / Unlock Slot", Keyboard.KEY_L);
    private Data data;

    public SlotLockingMod() {
        super("slotlocking", "Slot Locking", "Keeps your precious items safe.", Category.INVENTORY);
    }

    private Data data() {
        if (data == null) data = Config.readJson("slotlocking_data.json", Data.class, new Data());
        return data;
    }

    private boolean isLocked(Slot slot) {
        return slot != null && slot.inventory == mc.thePlayer.inventory && data().locked.contains(slot.getSlotIndex());
    }

    @Override
    protected void onEnable() {
        Hooks.slotClick.add(this);
        Hooks.dropItem.add(this);
    }

    @Override
    protected void onDisable() {
        Hooks.slotClick.remove(this);
        Hooks.dropItem.remove(this);
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer) || !Keyboard.getEventKeyState()) return;
        if (Keyboard.getEventKey() != key.getKeyCode()) return;
        Slot s = ((GuiContainerAccessor) event.gui).ml$getHoveredSlot();
        if (s == null || s.inventory != mc.thePlayer.inventory) return;
        int idx = s.getSlotIndex();
        if (!data().locked.remove(idx)) data().locked.add(idx);
        Config.writeJson("slotlocking_data.json", data());
        mc.thePlayer.playSound("random.click", 0.5f, data().locked.contains(idx) ? 1.2f : 0.8f);
    }

    @Override
    public boolean onClick(GuiContainer gui, Slot slot, int slotId, int button, int clickType) {
        if (isLocked(slot)) return true;
        // Number-key swaps (clickType 2) that would move a locked hotbar item.
        if (clickType == 2 && data().locked.contains(button)) return true;
        return false;
    }

    @Override
    public boolean onDrop(ItemStack held, boolean fullStack) {
        return data().locked.contains(mc.thePlayer.inventory.currentItem);
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainer) || data().locked.isEmpty()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        GuiContainerAccessor a = (GuiContainerAccessor) gui;
        GlStateManager.disableDepth();
        for (Slot s : gui.inventorySlots.inventorySlots) {
            if (!isLocked(s)) continue;
            int x = a.ml$getGuiLeft() + s.xDisplayPosition, y = a.ml$getGuiTop() + s.yDisplayPosition;
            Gui.drawRect(x, y, x + 16, y + 16, color);
            mc.fontRendererObj.drawStringWithShadow("⚿", x + 10, y - 1, 0xFFFFFFFF);
        }
        GlStateManager.enableDepth();
    }
}
