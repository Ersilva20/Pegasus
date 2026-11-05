package dev.mylauncher.core.gui;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.ClientSettings.MenuLayout;
import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Right-Shift menu in the Pegasus pixel style: category tabs, search, COSMETICS / SETTINGS tabs and the mods in one
 * of three layouts (picked with the buttons at the top right, like the view buttons of a file explorer):
 * CARDS (small cards with an icon), GRID (3 columns of 3:4 tiles with a live preview) and LIST (wide rows, no icons).
 */
public class ModMenuScreen extends GuiScreen {
    private static final int CARD_W = 112;
    private static final int CARD_H = 34;
    private static final int LIST_H = 30;
    private static final int GAP = 4;
    private static final int GRID_GAP = 6;
    private static final int SIDE = 78;
    private static final int VIEW_BTN = 16;
    private static final String[] LAYOUT_NAMES = {"CARDS", "GRID", "LIST"};

    private static Category lastCategory = null;
    private static String lastSearch = "";

    private GuiTextField search;
    private Category category = lastCategory;
    private int scroll;
    private int px, py, pw, ph;
    private float partialTicks;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(width - 16, SIDE + 12 + 3 * (CARD_W + GAP) + 6);
        ph = Math.min(height - 16, 286);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        search = new GuiTextField(0, fontRendererObj, gridLeft() + 4, py + 24, searchW() - 20, 10);
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

    // ---------------------------------------------------------------- layout

    private static MenuLayout layout() {
        return MLCore.settings != null ? MLCore.settings.menuLayout : MenuLayout.CARDS;
    }

    private int gridLeft() {
        return px + SIDE + 8;
    }

    private int gridRight() {
        return px + pw - 8;
    }

    /** The search box takes whatever the COSMETICS / SETTINGS tabs and the layout buttons leave free (max 140). */
    private int searchW() {
        return Math.max(50, Math.min(140, viewButtonsX() - 6 - 150 - 6 - gridLeft()));
    }

    private int cosmeticsX() {
        return gridLeft() + searchW() + 6;
    }

    private int settingsX() {
        return cosmeticsX() + 78;
    }

    private int viewButtonsX() {
        return px + pw - 6 - 3 * VIEW_BTN - 4;
    }

    private int gridTop() {
        return py + 40;
    }

    private int viewH() {
        return py + ph - 6 - gridTop();
    }

    private int gap() {
        return layout() == MenuLayout.GRID ? GRID_GAP : GAP;
    }

    private int columns() {
        switch (layout()) {
            case GRID: return 3;
            case LIST: return 1;
            default: return Math.max(1, (gridRight() + 2 - gridLeft() + GAP) / (CARD_W + GAP));
        }
    }

    private int cellW() {
        switch (layout()) {
            case GRID: return (gridRight() - gridLeft() - 2 * GRID_GAP) / 3;
            case LIST: return gridRight() - gridLeft();
            default: return CARD_W;
        }
    }

    private int cellH() {
        switch (layout()) {
            case GRID: return cellW() * 4 / 3;
            case LIST: return LIST_H;
            default: return CARD_H;
        }
    }

    private int cellX(int i) {
        return gridLeft() + (i % columns()) * (cellW() + gap());
    }

    private int cellY(int i) {
        return gridTop() + (i / columns()) * (cellH() + gap()) - scroll;
    }

    private void setLayout(MenuLayout l) {
        if (MLCore.settings == null || layout() == l) return;
        MLCore.settings.menuLayout = l;
        Config.save(MLCore.settings);
        scroll = 0;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.partialTicks = partialTicks;
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

        // Search, tabs and layout buttons
        Pixel.well(gridLeft(), py + 20, gridLeft() + searchW(), py + 36);
        search.drawTextBox();
        if (search.getText().isEmpty() && !search.isFocused()) fontRendererObj.drawString("SEARCH...", gridLeft() + 4, py + 24, 0xFF6D6079);
        Pixel.button(cosmeticsX(), py + 19, 76, 18, "COSMETICS", Pixel.Style.BLUE, mouseX, mouseY);
        Pixel.button(settingsX(), py + 19, 70, 18, "SETTINGS", Pixel.Style.BLUE, mouseX, mouseY);
        String layoutTip = null;
        for (int i = 0; i < 3; i++) {
            if (viewButton(MenuLayout.values()[i], viewButtonsX() + i * (VIEW_BTN + 2), py + 20, mouseX, mouseY)) layoutTip = LAYOUT_NAMES[i];
        }

        // Mods
        List<Feature> list = visible();
        int cols = columns();
        int rows = (list.size() + cols - 1) / cols;
        int contentH = rows * (cellH() + gap()) - gap();
        int maxScroll = Math.max(0, contentH - viewH());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        boolean inView = mouseY >= gridTop() && mouseY < gridTop() + viewH();
        int mx = inView ? mouseX : -1, my = inView ? mouseY : -1;
        scissor(gridLeft(), gridTop(), px + pw - 6, gridTop() + viewH());
        for (int i = 0; i < list.size(); i++) {
            int x = cellX(i), y = cellY(i);
            if (y + cellH() < gridTop() || y > gridTop() + viewH()) continue;
            switch (layout()) {
                case GRID: tile(list.get(i), x, y, mx, my); break;
                case LIST: row(list.get(i), x, y, mx, my); break;
                default: card(list.get(i), x, y, mx, my);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (list.isEmpty()) drawCenteredString(fontRendererObj, "NO MODS FOUND", gridLeft() + (px + pw - gridLeft()) / 2, gridTop() + 30, Pixel.DIM);
        if (maxScroll > 0) { // scrollbar
            int bx = px + pw - 5, bh = Math.max(12, viewH() * viewH() / contentH);
            int by = gridTop() + (viewH() - bh) * scroll / maxScroll;
            drawRect(bx, gridTop(), bx + 2, gridTop() + viewH(), Pixel.PANEL_LO);
            drawRect(bx, by, bx + 2, by + bh, Pixel.GRAY_HI);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (layoutTip != null) drawHoveringText(Collections.singletonList(layoutTip), mouseX, mouseY);
    }

    /** Scissor in GUI coordinates (top-left origin). */
    private void scissor(int x1, int y1, int x2, int y2) {
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x1 * f, mc.displayHeight - y2 * f, Math.max(0, x2 - x1) * f, Math.max(0, y2 - y1) * f);
    }

    private int tab(Category c, String label, int y, int mx, int my) {
        Pixel.button(px + 5, y, SIDE, 15, label, category == c ? Pixel.Style.YELLOW : Pixel.Style.GRAY, mx, my);
        return y + 17;
    }

    /** One of the three layout buttons, with a small pixel glyph of the layout. Returns true if hovered. */
    private boolean viewButton(MenuLayout l, int x, int y, int mx, int my) {
        boolean active = layout() == l;
        Pixel.Style s = active ? Pixel.Style.YELLOW : Pixel.Style.GRAY;
        boolean hover = Pixel.button(x, y, VIEW_BTN, VIEW_BTN, "", s, mx, my);
        int c = s.text, gx = x + 4, gy = y + 4 + (hover && Mouse.isButtonDown(0) ? 1 : 0);
        switch (l) {
            case CARDS: // 2 x 2 small cards
                drawRect(gx, gy, gx + 3, gy + 3, c);
                drawRect(gx + 5, gy, gx + 8, gy + 3, c);
                drawRect(gx, gy + 5, gx + 3, gy + 8, c);
                drawRect(gx + 5, gy + 5, gx + 8, gy + 8, c);
                break;
            case GRID: // 3 tall tiles
                for (int i = 0; i < 3; i++) drawRect(gx + i * 3, gy, gx + i * 3 + 2, gy + 8, c);
                break;
            default: // rows
                for (int i = 0; i < 3; i++) drawRect(gx, gy + i * 3, gx + 8, gy + i * 3 + 2, c);
        }
        return hover;
    }

    private static boolean hasConfig(Feature f) {
        return !f.settings().isEmpty() || f.customScreen(null) != null;
    }

    /** CARDS: the original small card, with the mod's item icon on a colored tile. */
    private void card(Feature f, int x, int y, int mx, int my) {
        boolean hover = hit(mx, my, x, y, CARD_W, CARD_H);
        Pixel.box(x, y, x + CARD_W, y + CARD_H, hover ? 0xFF241D2C : Pixel.PANEL_LO, 0xFF2C2436, 0);
        int ic = Pixel.colorFor(f.name);
        Pixel.box(x + 4, y + 7, x + 24, y + 27, Pixel.darken(ic, 0.5f), Pixel.darken(ic, 0.75f), Pixel.darken(ic, 0.3f));
        ModIcons.draw(f, x + 6, y + 9, 16);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(f.name, CARD_W - 50), x + 28, y + 8, Pixel.CREAM);
        String sub = f.category.display.toUpperCase() + (f.customScreen(null) != null ? "  ▶" : f.settings().isEmpty() ? "" : "  ⚙");
        fontRendererObj.drawString(sub, x + 28, y + 19, Pixel.DIM);
        Pixel.toggle(x + CARD_W - 20, y + 10, 15, f.isEnabled(), false);
    }

    /** GRID: a 3:4 tile with icon + name + gear on top, a preview of the mod in the middle and an on/off button. */
    private void tile(Feature f, int x, int y, int mx, int my) {
        int w = cellW(), h = cellH();
        boolean hover = hit(mx, my, x, y, w, h);
        Pixel.box(x, y, x + w, y + h, hover ? 0xFF241D2C : Pixel.PANEL_LO, 0xFF2C2436, 0);

        // Header
        ModIcons.draw(f, x + 5, y + 5, 16);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(f.name, w - 42), x + 25, y + 9, Pixel.CREAM);
        boolean gearHover = hit(mx, my, x + w - 17, y + 4, 13, 17);
        Pixel.gear(x + w - 14, y + 8, gearHover ? Pixel.COPPER_HI : hover ? Pixel.CREAM : Pixel.DIM);
        drawRect(x + 4, y + 24, x + w - 4, y + 25, 0xFF2C2436);

        // Preview
        int wx1 = x + 5, wy1 = y + 28, wx2 = x + w - 5, wy2 = y + h - 27;
        Pixel.well(wx1, wy1, wx2, wy2);
        preview(f, wx1 + 1, wy1 + 1, wx2 - 1, wy2 - 1);

        // On / off
        boolean on = f.isEnabled();
        Pixel.button(x + 5, y + h - 23, w - 10, 18, on ? "ENABLED" : "DISABLED", on ? Pixel.Style.GREEN : Pixel.Style.RED, mx, my);
    }

    /** Live render of a HUD mod (its sample content when there is nothing to show), else a big icon + description. */
    private void preview(Feature f, int x1, int y1, int x2, int y2) {
        int aw = x2 - x1, ah = y2 - y1;
        if (f instanceof HudFeature) {
            HudFeature hud = (HudFeature) f;
            int hw = Math.max(1, hud.width()), hh = Math.max(1, hud.height());
            float s = Math.min(1.5f, Math.min((aw - 8) / (float) hw, (ah - 8) / (float) hh));
            scissor(Math.max(x1, gridLeft()), Math.max(y1, gridTop()), x2, Math.min(y2, gridTop() + viewH()));
            GlStateManager.pushMatrix();
            GlStateManager.translate(x1 + (aw - hw * s) / 2f, y1 + (ah - hh * s) / 2f, 0);
            GlStateManager.scale(s, s, 1);
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);
            boolean ok = true;
            try {
                hud.render(partialTicks, true);
            } catch (Exception e) {
                ok = false;
            }
            GlStateManager.popMatrix();
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.color(1, 1, 1, 1);
            scissor(gridLeft(), gridTop(), px + pw - 6, gridTop() + viewH());
            if (ok) return;
        }
        int icon = 32;
        List<String> lines = fontRendererObj.listFormattedStringToWidth(f.description, aw - 8);
        int maxLines = Math.max(0, (ah - icon - 10) / 10);
        if (lines.size() > maxLines) {
            lines = new ArrayList<String>(lines.subList(0, maxLines));
            if (maxLines > 0) {
                String last = lines.get(maxLines - 1);
                lines.set(maxLines - 1, fontRendererObj.trimStringToWidth(last, aw - 8 - fontRendererObj.getStringWidth("...")) + "...");
            }
        }
        int blockH = icon + 6 + lines.size() * 10;
        int top = y1 + (ah - blockH) / 2;
        ModIcons.draw(f, x1 + (aw - icon) / 2, top, icon);
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            fontRendererObj.drawString(l, x1 + (aw - fontRendererObj.getStringWidth(l)) / 2, top + icon + 6 + i * 10, Pixel.DIM);
        }
    }

    /** LIST: a wide row with name, category, description, gear and toggle (no icon). */
    private void row(Feature f, int x, int y, int mx, int my) {
        int w = cellW();
        boolean hover = hit(mx, my, x, y, w, LIST_H);
        Pixel.box(x, y, x + w, y + LIST_H, hover ? 0xFF241D2C : Pixel.PANEL_LO, 0xFF2C2436, 0);
        drawRect(x + 1, y + 2, x + 3, y + LIST_H - 2, f.isEnabled() ? Pixel.GREEN : Pixel.GRAY);
        String name = f.name.toUpperCase();
        fontRendererObj.drawString(name, x + 9, y + 6, Pixel.CREAM);
        Pixel.tag(x + 15 + fontRendererObj.getStringWidth(name), y + 5, f.category.display.toUpperCase(), Pixel.GRAY);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(f.description, w - 60), x + 9, y + 17, Pixel.DIM);
        if (hasConfig(f)) {
            boolean gearHover = hit(mx, my, x + w - 42, y + 7, 14, 16);
            Pixel.gear(x + w - 39, y + 10, gearHover ? Pixel.COPPER_HI : hover ? Pixel.CREAM : Pixel.DIM);
        }
        Pixel.toggle(x + w - 22, y + 7, 16, f.isEnabled(), false);
    }

    // ---------------------------------------------------------------- input

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (mouseX >= gridLeft() && mouseX < gridLeft() + searchW() && mouseY >= py + 20 && mouseY < py + 36) {
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
        for (int i = 0; i < 3; i++) {
            if (hit(mouseX, mouseY, viewButtonsX() + i * (VIEW_BTN + 2), py + 20, VIEW_BTN, VIEW_BTN)) {
                setLayout(MenuLayout.values()[i]);
                click(1.1f);
                return;
            }
        }
        if (mouseY < gridTop() || mouseY >= gridTop() + viewH()) return;
        List<Feature> list = visible();
        int w = cellW(), h = cellH();
        for (int i = 0; i < list.size(); i++) {
            int x = cellX(i), y = cellY(i);
            if (!hit(mouseX, mouseY, x, y, w, h)) continue;
            Feature f = list.get(i);
            boolean onToggle;
            switch (layout()) {
                case GRID: onToggle = mouseY >= y + h - 24; break;
                case LIST: onToggle = mouseX >= x + w - 26; break;
                default: onToggle = mouseX >= x + w - 22;
            }
            if (onToggle || button == 1) {
                f.toggle();
                click(f.isEnabled() ? 1.3f : 0.9f);
                return;
            }
            GuiScreen custom = f.customScreen(this);
            if (custom != null) mc.displayGuiScreen(custom);
            else if (layout() == MenuLayout.GRID || !f.settings().isEmpty()) mc.displayGuiScreen(new SettingsScreen(this, f));
            else {
                f.toggle();
                click(f.isEnabled() ? 1.3f : 0.9f);
            }
            return;
        }
    }

    private void click(float pitch) {
        if (mc.thePlayer != null) mc.thePlayer.playSound("random.click", 0.3f, pitch);
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll -= Integer.signum(wheel) * Math.min(cellH() + gap(), 48);
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
