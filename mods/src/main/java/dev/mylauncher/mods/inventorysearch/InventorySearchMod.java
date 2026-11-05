package dev.mylauncher.mods.inventorysearch;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.mixin.GuiContainerAccessor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * A search bar above every container. Matching items stay bright, others are dimmed.
 * Syntax: "|" = OR, "&amp;" = AND, "!" = NOT, "\" escapes one of those characters. Searches name and tooltip.
 */
@Mod(modid = "ml_inventorysearch", name = "Inventory Search", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class InventorySearchMod extends Feature {
    @Setting(name = "Search Tooltips Too")
    public boolean tooltips = true;
    @Setting(name = "Dim Color", color = true)
    public int dim = 0xB0000000;
    @Setting(name = "Remember Search")
    public boolean remember = false;

    private GuiTextField field;
    private String text = "";

    public InventorySearchMod() {
        super("inventorysearch", "Inventory Search", "Adds a search bar to inventories.", Category.INVENTORY);
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        if (event.gui == null && !remember) text = "";
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiContainer) || event.gui instanceof GuiContainerCreative) {
            field = null;
            return;
        }
        GuiContainerAccessor a = (GuiContainerAccessor) event.gui;
        int w = Math.min(120, a.ml$getXSize());
        field = new GuiTextField(9001, mc.fontRendererObj, a.ml$getGuiLeft() + (a.ml$getXSize() - w) / 2, a.ml$getGuiTop() - 16, w, 12);
        field.setMaxStringLength(64);
        field.setText(text);
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (field == null || !(event.gui instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.gui;
        GuiContainerAccessor a = (GuiContainerAccessor) gui;
        String q = field.getText().trim();
        if (!q.isEmpty()) {
            GlStateManager.disableDepth();
            for (Slot s : gui.inventorySlots.inventorySlots) {
                if (matches(s.getStack(), q)) continue;
                int x = a.ml$getGuiLeft() + s.xDisplayPosition, y = a.ml$getGuiTop() + s.yDisplayPosition;
                Gui.drawRect(x, y, x + 16, y + 16, dim);
            }
            GlStateManager.enableDepth();
        }
        field.drawTextBox();
        if (field.getText().isEmpty() && !field.isFocused()) {
            mc.fontRendererObj.drawStringWithShadow("Search...", field.xPosition + 4, field.yPosition + 2, 0xFF888888);
        }
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (field == null || !Mouse.getEventButtonState()) return;
        int mx = Mouse.getEventX() * event.gui.width / mc.displayWidth;
        int my = event.gui.height - Mouse.getEventY() * event.gui.height / mc.displayHeight - 1;
        boolean inside = mx >= field.xPosition && mx < field.xPosition + field.width && my >= field.yPosition && my < field.yPosition + field.height;
        field.mouseClicked(mx, my, Mouse.getEventButton());
        if (inside && Mouse.getEventButton() == 1) {
            field.setText("");
            text = "";
        }
        if (inside) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (field == null || !field.isFocused() || !Keyboard.getEventKeyState()) return;
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE) {
            field.setFocused(false);
        } else {
            field.textboxKeyTyped(Keyboard.getEventCharacter(), key);
            text = field.getText();
        }
        event.setCanceled(true);
    }

    private boolean matches(ItemStack stack, String query) {
        if (stack == null) return false;
        StringBuilder hay = new StringBuilder(stack.getDisplayName());
        if (tooltips) {
            try {
                List<String> lines = stack.getTooltip(mc.thePlayer, false);
                for (String l : lines) hay.append('\n').append(l);
            } catch (Exception ignored) {
            }
        }
        String h = EnumChatFormatting.getTextWithoutFormattingCodes(hay.toString()).toLowerCase();
        return evalOr(query.toLowerCase(), h);
    }

    private static boolean evalOr(String q, String hay) {
        for (String part : split(q, '|')) if (evalAnd(part, hay)) return true;
        return false;
    }

    private static boolean evalAnd(String q, String hay) {
        for (String part : split(q, '&')) {
            String t = part.trim();
            boolean not = t.startsWith("!");
            if (not) t = t.substring(1).trim();
            t = t.replace("\\|", "|").replace("\\&", "&").replace("\\!", "!");
            boolean found = t.isEmpty() || hay.contains(t);
            if (found == not) return false;
        }
        return true;
    }

    private static java.util.List<String> split(String s, char sep) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                cur.append(c).append(s.charAt(++i));
            } else if (c == sep) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
