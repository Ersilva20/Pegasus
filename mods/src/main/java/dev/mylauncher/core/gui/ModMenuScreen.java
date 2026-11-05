package dev.mylauncher.core.gui;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Right-Shift menu in the Pegasus pixel style: category tabs, search, COSMETICS / SETTINGS tabs and a grid of mod cards. */
public class ModMenuScreen extends GuiScreen {
    private static final int CARD_W = 112;
    private static final int CARD_H = 34;
    private static final int GAP = 4;
    private static final int SIDE = 78;

    private static Category lastCategory = null;
    private static String lastSearch = "";

    private GuiTextField search;
    private Category category = lastCategory;
    private int scroll;
    private int px, py, pw, ph;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(width - 16, SIDE + 12 + 3 * (CARD_W + GAP) + 6);
        ph = Math.min(height - 16, 286);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        search = new GuiTextField(0, fontRendererObj, gridLeft() + 4, py + 24, 120, 10);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(40);
        search.setText(lastSearch);
        search.setFocused(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        lastCategory = category;
        lastSearch = search.getText();
    }

    private List<Feature> visible() {
        List<Feature> out = new ArrayList<Feature>();
        String q = search.getText().trim().toLowerCase();
        for (Feature f : MLCore.features()) {
            if (f.builtIn()) continue;
            if (category != null && f.category != category) continue;
            if (!q.isEmpty() && !f.name.toLowerCase().contains(q) && !f.description.toLowerCase().contains(q)) continue;
            out.add(f);
        }
        return out;
    }

    private int gridLeft() {
        return px + SIDE + 8;
    }

    private int cosmeticsX() {
        return gridLeft() + 146;
    }

    private int settingsX() {
        return cosmeticsX() + 80;
    }

    private int gridTop() {
        return py + 40;
    }

    private int viewH() {
        return py + ph - 6 - gridTop();
    }

    private int columns() {
        return Math.max(1, (px + pw - 6 - gridLeft() + GAP) / (CARD_W + GAP));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Pixel.overlay(width, height);
        int on = 0, total = 0;
        for (Feature f : MLCore.features()) {
            if (f.builtIn()) continue;
            total++;
            if (f.isEnabled()) on++;
        }
        Pixel.window(px, py, px + pw, py + ph, "PEGASUS MODS", on + " / " + total + " ON");
        Pixel.logo(px + 3, py + 3, 10);

        // Sidebar tabs
        int ty = py + 22;
        ty = tab(null, "ALL MODS", ty, mouseX, mouseY);
        for (Category c : Category.values()) ty = tab(c, c.display.toUpperCase(), ty, mouseX, mouseY);
        Pixel.button(px + 5, py + ph - 24, SIDE, 18, "EDIT HUD", Pixel.Style.COPPER, mouseX, mouseY);

        // Search
        Pixel.well(gridLeft(), py + 20, gridLeft() + 140, py + 36);
        search.drawTextBox();
        if (search.getText().isEmpty() && !search.isFocused()) fontRendererObj.drawString("SEARCH...", gridLeft() + 4, py + 24, 0xFF6D6079);
        Pixel.button(cosmeticsX(), py + 19, 76, 18, "COSMETICS", Pixel.Style.BLUE, mouseX, mouseY);
        Pixel.button(settingsX(), py + 19, 70, 18, "SETTINGS", Pixel.Style.BLUE, mouseX, mouseY);

        // Cards
        List<Feature> list = visible();
        int cols = columns();
        int rows = (list.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows * (CARD_H + GAP) - viewH());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(gridLeft() * f, mc.displayHeight - (gridTop() + viewH()) * f, (px + pw - gridLeft()) * f, viewH() * f);
        for (int i = 0; i < list.size(); i++) {
            int x = gridLeft() + (i % cols) * (CARD_W + GAP);
            int y = gridTop() + (i / cols) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < gridTop() || y > gridTop() + viewH()) continue;
            card(list.get(i), x, y, mouseX, mouseY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (list.isEmpty()) drawCenteredString(fontRendererObj, "NO MODS FOUND", gridLeft() + (px + pw - gridLeft()) / 2, gridTop() + 30, Pixel.DIM);
        if (maxScroll > 0) { // scrollbar
            int bx = px + pw - 5, bh = Math.max(12, viewH() * viewH() / (rows * (CARD_H + GAP)));
            int by = gridTop() + (viewH() - bh) * scroll / maxScroll;
            drawRect(bx, gridTop(), bx + 2, gridTop() + viewH(), Pixel.PANEL_LO);
            drawRect(bx, by, bx + 2, by + bh, Pixel.GRAY_HI);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int tab(Category c, String label, int y, int mx, int my) {
        Pixel.button(px + 5, y, SIDE, 15, label, category == c ? Pixel.Style.YELLOW : Pixel.Style.GRAY, mx, my);
        return y + 17;
    }

    private void card(Feature f, int x, int y, int mx, int my) {
        boolean hover = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H && my >= gridTop();
        Pixel.box(x, y, x + CARD_W, y + CARD_H, hover ? 0xFF241D2C : Pixel.PANEL_LO, 0xFF2C2436, 0);
        int ic = Pixel.colorFor(f.name);
        Pixel.box(x + 4, y + 7, x + 24, y + 27, ic, Pixel.lighten(ic, 0.3f), Pixel.darken(ic, 0.7f));
        String letters = f.name.replaceAll("[^A-Za-z0-9]", "");
        letters = letters.length() > 1 ? letters.substring(0, 2).toUpperCase() : letters.toUpperCase();
        fontRendererObj.drawStringWithShadow(letters, x + 14 - fontRendererObj.getStringWidth(letters) / 2f, y + 13, 0xFFFFFFFF);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(f.name, CARD_W - 50), x + 28, y + 8, Pixel.CREAM);
        String sub = f.category.display.toUpperCase() + (f.customScreen(null) != null ? "  ▶" : f.settings().isEmpty() ? "" : "  ⚙");
        fontRendererObj.drawString(sub, x + 28, y + 19, Pixel.DIM);
        Pixel.toggle(x + CARD_W - 20, y + 10, 15, f.isEnabled(), false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (mouseX >= gridLeft() && mouseX < gridLeft() + 140 && mouseY >= py + 20 && mouseY < py + 36) {
            search.setFocused(true);
        }
        int ty = py + 22;
        if (hit(mouseX, mouseY, px + 5, ty, SIDE, 15)) { category = null; scroll = 0; return; }
        for (Category c : Category.values()) {
            ty += 17;
            if (hit(mouseX, mouseY, px + 5, ty, SIDE, 15)) { category = c; scroll = 0; return; }
        }
        if (hit(mouseX, mouseY, px + 5, py + ph - 24, SIDE, 18)) {
            mc.displayGuiScreen(new HudEditorScreen(this));
            return;
        }
        if (hit(mouseX, mouseY, cosmeticsX(), py + 19, 76, 18) && MLCore.cosmetics != null) {
            mc.displayGuiScreen(MLCore.cosmetics.customScreen(this));
            return;
        }
        if (hit(mouseX, mouseY, settingsX(), py + 19, 70, 18) && MLCore.settings != null) {
            mc.displayGuiScreen(new SettingsScreen(this, MLCore.settings));
            return;
        }
        if (mouseY < gridTop() || mouseY >= gridTop() + viewH()) return;
        List<Feature> list = visible();
        int cols = columns();
        for (int i = 0; i < list.size(); i++) {
            int x = gridLeft() + (i % cols) * (CARD_W + GAP);
            int y = gridTop() + (i / cols) * (CARD_H + GAP) - scroll;
            if (!hit(mouseX, mouseY, x, y, CARD_W, CARD_H)) continue;
            Feature f = list.get(i);
            boolean onToggle = mouseX >= x + CARD_W - 22;
            if (onToggle || button == 1) {
                f.toggle();
                if (mc.thePlayer != null) mc.thePlayer.playSound("random.click", 0.3f, f.isEnabled() ? 1.3f : 0.9f);
                return;
            }
            GuiScreen custom = f.customScreen(this);
            if (custom != null) mc.displayGuiScreen(custom);
            else if (!f.settings().isEmpty()) mc.displayGuiScreen(new SettingsScreen(this, f));
            else f.toggle();
            return;
        }
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll -= Integer.signum(wheel) * (CARD_H + GAP);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || (keyCode == MLCore.menuKey.getKeyCode() && search.getText().isEmpty())) {
            mc.displayGuiScreen(null);
            return;
        }
        if (search.textboxKeyTyped(typedChar, keyCode)) scroll = 0;
    }

    @Override
    public void updateScreen() {
        search.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
