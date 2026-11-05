package dev.mylauncher.mods.crosshair;

import dev.mylauncher.core.Config;
import dev.mylauncher.core.SettingField;
import dev.mylauncher.core.gui.ColorPickerScreen;
import dev.mylauncher.core.gui.Pixel;
import dev.mylauncher.core.gui.SettingsScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import static dev.mylauncher.mods.crosshair.CrosshairMod.GRID;
import static dev.mylauncher.mods.crosshair.CrosshairMod.MID;

/**
 * Draw your own crosshair pixel by pixel: left click / drag draws, right click / drag erases. The middle cell is the
 * center of the screen. Mirror, presets (the built-in styles as a starting point), undo (Ctrl+Z), color and a live
 * preview at 1x and 2x. Drawing switches the Style setting to CUSTOM.
 */
public class CrosshairEditorScreen extends GuiScreen {
    private static final CrosshairMod.Style[] PRESETS = {
            CrosshairMod.Style.CROSS, CrosshairMod.Style.CROSS_DOT, CrosshairMod.Style.DOT, CrosshairMod.Style.T_SHAPE,
            CrosshairMod.Style.CIRCLE, CrosshairMod.Style.SQUARE, CrosshairMod.Style.X};

    private final GuiScreen parent;
    private final CrosshairMod mod;
    private final Deque<String> undo = new ArrayDeque<String>();
    private static boolean mirrorX = true, mirrorY = true;
    private int preset = -1;
    private Boolean painting; // null = not dragging, else the value being painted
    private int px, py, pw, ph, cs, gx, gy, rx, rw;

    public CrosshairEditorScreen(GuiScreen parent, CrosshairMod mod) {
        this.parent = parent;
        this.mod = mod;
    }

    @Override
    public void initGui() {
        pw = Math.min(width - 16, 340);
        ph = Math.min(height - 16, 230);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cs = Math.max(4, Math.min(11, (ph - 52) / GRID));
        gx = px + 10;
        gy = py + 44;
        rx = gx + cs * GRID + 14;
        rw = px + pw - 8 - rx;
    }

    @Override
    public void onGuiClosed() {
        Config.save(mod);
    }

    // ---------------------------------------------------------------- layout of the right column

    private int previewBottom() {
        return gy + 60;
    }

    private int rowY(int i) {
        return previewBottom() + 8 + i * 20;
    }

    private int halfW() {
        return (rw - 4) / 2;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Pixel.overlay(width, height);
        Pixel.window(px, py, px + pw, py + ph, "Crosshair Editor", "LEFT DRAW - RIGHT ERASE");
        Pixel.button(px + 6, py + 21, 48, 16, "< BACK", Pixel.Style.GRAY, mouseX, mouseY);
        boolean inUse = mod.style == CrosshairMod.Style.CUSTOM;
        String status = inUse ? "IN USE" : "NOT IN USE (STYLE " + mod.style.name().replace('_', ' ') + ")";
        fontRendererObj.drawString(status, px + 60, py + 25, inUse ? Pixel.GREEN : Pixel.DIM);
        Pixel.button(px + pw - 76, py + 21, 70, 16, "SETTINGS", Pixel.Style.BLUE, mouseX, mouseY);

        drawGrid(mouseX, mouseY);
        drawPreview();

        Pixel.button(rx, rowY(0), halfW(), 16, "MIRROR X", mirrorX ? Pixel.Style.YELLOW : Pixel.Style.GRAY, mouseX, mouseY);
        Pixel.button(rx + rw - halfW(), rowY(0), halfW(), 16, "MIRROR Y", mirrorY ? Pixel.Style.YELLOW : Pixel.Style.GRAY, mouseX, mouseY);
        String presetLabel = preset < 0 ? "LOAD PRESET" : "PRESET: " + PRESETS[preset].name().replace('_', ' ');
        Pixel.button(rx, rowY(1), rw, 16, presetLabel, Pixel.Style.COPPER, mouseX, mouseY);
        Pixel.button(rx, rowY(2), halfW(), 16, "UNDO", undo.isEmpty() ? Pixel.Style.GRAY : Pixel.Style.BLUE, mouseX, mouseY);
        Pixel.button(rx + rw - halfW(), rowY(2), halfW(), 16, "CLEAR", Pixel.Style.RED, mouseX, mouseY);
        Pixel.button(rx, rowY(3), halfW(), 16, "COLOR", Pixel.Style.GRAY, mouseX, mouseY);
        drawRect(rx + halfW() - 12, rowY(3) + 4, rx + halfW() - 4, rowY(3) + 12, 0xFF000000 | mod.color);
        Pixel.button(rx + rw - halfW(), rowY(3), halfW(), 16, inUse ? "IN USE" : "USE IT", inUse ? Pixel.Style.GREEN : Pixel.Style.GRAY, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawGrid(int mx, int my) {
        int size = cs * GRID;
        Pixel.well(gx - 3, gy - 3, gx + size + 3, gy + size + 3);
        boolean[][] cells = mod.cells();
        int on = 0xFF000000 | mod.color;
        int gapPx = cs >= 6 ? 1 : 0;
        int[] hover = cellAt(mx, my);
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int c;
                if (cells[y][x]) c = on;
                else if (x == MID && y == MID) c = 0xFF4A3530; // screen center
                else if (x == MID || y == MID) c = 0xFF2A2233; // center guides
                else c = ((x + y) & 1) == 0 ? 0xFF1A1420 : 0xFF1F1826;
                int x1 = gx + x * cs, y1 = gy + y * cs;
                drawRect(x1, y1, x1 + cs - gapPx, y1 + cs - gapPx, c);
            }
        }
        if (hover != null) {
            int x1 = gx + hover[0] * cs, y1 = gy + hover[1] * cs, x2 = x1 + cs - gapPx, y2 = y1 + cs - gapPx;
            int c = Pixel.COPPER_HI;
            drawRect(x1, y1, x2, y1 + 1, c);
            drawRect(x1, y2 - 1, x2, y2, c);
            drawRect(x1, y1, x1 + 1, y2, c);
            drawRect(x2 - 1, y1, x2, y2, c);
        }
    }

    private void drawPreview() {
        int mid = rx + rw / 2, top = gy, bottom = previewBottom();
        Pixel.well(rx, top, rx + rw, bottom);
        drawGradientRect(rx + 2, top + 2, mid, bottom - 2, 0xFF7FAEF5, 0xFFC4DBFF); // sky
        drawRect(mid, top + 2, rx + rw - 2, bottom - 2, 0xFF5A5A5A); // stone
        drawRect(mid, top + 2, mid + 1, bottom - 2, Pixel.OUTLINE);
        int cy = (top + bottom) / 2;
        preview((rx + 2 + mid) / 2, cy, 1);
        preview((mid + rx + rw - 2) / 2, cy, 2);
        fontRendererObj.drawString("1X", rx + 4, bottom - 11, 0xFF33475F);
        fontRendererObj.drawString("2X", rx + rw - 4 - fontRendererObj.getStringWidth("2X"), bottom - 11, Pixel.CREAM);
    }

    private void preview(int cx, int cy, int scale) {
        CrosshairMod.Style old = mod.style;
        mod.style = CrosshairMod.Style.CUSTOM;
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(scale, scale, 1);
        mod.render(0, 0, mod.color);
        GlStateManager.popMatrix();
        mod.style = old;
    }

    // ---------------------------------------------------------------- editing

    private int[] cellAt(int mx, int my) {
        if (mx < gx || my < gy) return null;
        int x = (mx - gx) / cs, y = (my - gy) / cs;
        return x < GRID && y < GRID ? new int[]{x, y} : null;
    }

    private void remember() {
        undo.push(mod.pattern);
        while (undo.size() > 100) undo.removeLast();
    }

    private void paint(int x, int y, boolean value) {
        boolean[][] cells = copy(mod.cells());
        cells[y][x] = value;
        if (mirrorX) cells[y][GRID - 1 - x] = value;
        if (mirrorY) cells[GRID - 1 - y][x] = value;
        if (mirrorX && mirrorY) cells[GRID - 1 - y][GRID - 1 - x] = value;
        apply(cells);
    }

    private void apply(boolean[][] cells) {
        mod.setCells(cells);
        mod.style = CrosshairMod.Style.CUSTOM;
    }

    private static boolean[][] copy(boolean[][] src) {
        boolean[][] out = new boolean[GRID][GRID];
        for (int y = 0; y < GRID; y++) System.arraycopy(src[y], 0, out[y], 0, GRID);
        return out;
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void click() {
        if (mc.thePlayer != null) mc.thePlayer.playSound("random.click", 0.3f, 1.2f);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        int[] cell = cellAt(mx, my);
        if (cell != null && (button == 0 || button == 1)) {
            remember();
            painting = button == 0;
            paint(cell[0], cell[1], painting);
            return;
        }
        if (button != 0) return;
        if (hit(mx, my, px + 6, py + 21, 48, 16)) {
            mc.displayGuiScreen(parent);
        } else if (hit(mx, my, px + pw - 76, py + 21, 70, 16)) {
            mc.displayGuiScreen(new SettingsScreen(this, mod));
        } else if (hit(mx, my, rx, rowY(0), halfW(), 16)) {
            mirrorX = !mirrorX;
            click();
        } else if (hit(mx, my, rx + rw - halfW(), rowY(0), halfW(), 16)) {
            mirrorY = !mirrorY;
            click();
        } else if (hit(mx, my, rx, rowY(1), rw, 16)) {
            preset = (preset + 1) % PRESETS.length;
            remember();
            apply(mod.rasterize(PRESETS[preset]));
            click();
        } else if (hit(mx, my, rx, rowY(2), halfW(), 16)) {
            undo();
        } else if (hit(mx, my, rx + rw - halfW(), rowY(2), halfW(), 16)) {
            remember();
            apply(new boolean[GRID][GRID]);
            click();
        } else if (hit(mx, my, rx, rowY(3), halfW(), 16)) {
            for (SettingField sf : mod.settings()) {
                if (sf.key().equals("color")) mc.displayGuiScreen(new ColorPickerScreen(this, mod, sf));
            }
        } else if (hit(mx, my, rx + rw - halfW(), rowY(3), halfW(), 16)) {
            mod.style = CrosshairMod.Style.CUSTOM;
            click();
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        int[] cell = cellAt(mx, my);
        if (painting != null && cell != null) {
            boolean current = mod.cells()[cell[1]][cell[0]];
            if (current != painting) paint(cell[0], cell[1], painting);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        painting = null;
    }

    private void undo() {
        if (undo.isEmpty()) return;
        mod.pattern = undo.pop();
        click();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        } else if (keyCode == Keyboard.KEY_Z && isCtrlKeyDown()) {
            undo();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
