package dev.mylauncher.core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * Pegasus pixel/retro UI kit, matching the launcher: dark plum panels, 1-unit outlines with notched
 * corners, bevelled buttons (copper / green / red / yellow), square toggles and segmented bars.
 */
public final class Pixel {
    private Pixel() {
    }

    // Launcher palette
    public static final int OUTLINE = 0xFF0D0910;
    public static final int PANEL = 0xFF2A2233;
    public static final int PANEL_HI = 0xFF3B3147;
    public static final int PANEL_LO = 0xFF1D1724;
    public static final int WELL = 0xFF120D17;
    public static final int CREAM = 0xFFF3E9D2;
    public static final int DIM = 0xFFA99BB5;
    public static final int COPPER = 0xFFD9823B, COPPER_HI = 0xFFF2A660, COPPER_LO = 0xFFA35A22;
    public static final int GREEN = 0xFF3CCB4F, GREEN_HI = 0xFF79E685, GREEN_LO = 0xFF22913A;
    public static final int RED = 0xFFD6403A, RED_HI = 0xFFF0705F, RED_LO = 0xFF962824;
    public static final int BLUE = 0xFF3FB5F0, BLUE_HI = 0xFF8FD8FF, BLUE_LO = 0xFF1F6FA8;
    public static final int YELLOW = 0xFFF2C14E, YELLOW_HI = 0xFFFFE08F, YELLOW_LO = 0xFFB88A1E;
    public static final int GRAY = 0xFF4A3F58, GRAY_HI = 0xFF64577A, GRAY_LO = 0xFF2F2738;

    public enum Style {
        GRAY(Pixel.GRAY, GRAY_HI, GRAY_LO, CREAM),
        COPPER(Pixel.COPPER, COPPER_HI, COPPER_LO, 0xFF2B1608),
        GREEN(Pixel.GREEN, GREEN_HI, GREEN_LO, 0xFF0C2A12),
        RED(Pixel.RED, RED_HI, RED_LO, CREAM),
        BLUE(Pixel.BLUE, BLUE_HI, BLUE_LO, 0xFF06263B),
        YELLOW(Pixel.YELLOW, YELLOW_HI, YELLOW_LO, 0xFF2B1608);

        final int fill, hi, lo, text;

        Style(int fill, int hi, int lo, int text) {
            this.fill = fill;
            this.hi = hi;
            this.lo = lo;
            this.text = text;
        }
    }

    private static final ResourceLocation LOGO = new ResourceLocation("mlcore", "textures/logo.png");

    private static FontRenderer font() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    /** Filled box with a 1-unit outline and notched (missing) corner pixels. */
    public static void box(int x1, int y1, int x2, int y2, int fill, int hi, int lo) {
        Gui.drawRect(x1 + 1, y1, x2 - 1, y1 + 1, OUTLINE);
        Gui.drawRect(x1 + 1, y2 - 1, x2 - 1, y2, OUTLINE);
        Gui.drawRect(x1, y1 + 1, x1 + 1, y2 - 1, OUTLINE);
        Gui.drawRect(x2 - 1, y1 + 1, x2, y2 - 1, OUTLINE);
        Gui.drawRect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
        if (hi != 0) Gui.drawRect(x1 + 1, y1 + 1, x2 - 1, y1 + 2, hi);
        if (lo != 0) Gui.drawRect(x1 + 1, y2 - 2, x2 - 1, y2 - 1, lo);
    }

    public static void panel(int x1, int y1, int x2, int y2) {
        box(x1, y1, x2, y2, PANEL, PANEL_HI, PANEL_LO);
    }

    public static void well(int x1, int y1, int x2, int y2) {
        box(x1, y1, x2, y2, WELL, 0xFF07050A, 0);
    }

    /** Retro window: panel + title bar "■ /TITLE" with optional right-side text. Returns the body top. */
    public static int window(int x1, int y1, int x2, int y2, String title, String meta) {
        panel(x1, y1, x2, y2);
        Gui.drawRect(x1 + 1, y1 + 1, x2 - 1, y1 + 15, PANEL_HI);
        Gui.drawRect(x1 + 1, y1 + 15, x2 - 1, y1 + 16, OUTLINE);
        Gui.drawRect(x1 + 6, y1 + 5, x1 + 11, y1 + 10, CREAM);
        Gui.drawRect(x1 + 7, y1 + 6, x1 + 10, y1 + 9, PANEL_HI);
        font().drawString("/" + title.toUpperCase(), x1 + 14, y1 + 4, CREAM, false);
        if (meta != null) font().drawString(meta, x2 - 6 - font().getStringWidth(meta), y1 + 4, DIM, false);
        return y1 + 16;
    }

    /** Bevelled button that moves down 1 unit while pressed. Returns true if hovered. */
    public static boolean button(int x, int y, int w, int h, String label, Style s, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        boolean down = hover && org.lwjgl.input.Mouse.isButtonDown(0);
        int oy = down ? 1 : 0;
        int fill = hover ? lighten(s.fill, 0.1f) : s.fill;
        box(x, y + oy, x + w, y + h + oy, fill, down ? s.lo : s.hi, down ? 0 : s.lo);
        int tw = font().getStringWidth(label);
        font().drawString(label, x + (w - tw) / 2, y + (h - 8) / 2 + oy, s.text, false);
        return hover;
    }

    /** Square on/off toggle: green with a check, or red with an X (like the launcher). */
    public static void toggle(int x, int y, int size, boolean on, boolean locked) {
        Style s = on ? Style.GREEN : Style.RED;
        box(x, y, x + size, y + size, locked ? darken(s.fill, 0.75f) : s.fill, s.hi, s.lo);
        int c = on ? 0xFF0C2A12 : CREAM;
        int cx = x + size / 2, cy = y + size / 2;
        if (on) { // pixel check mark
            Gui.drawRect(cx - 4, cy, cx - 3, cy + 1, c);
            Gui.drawRect(cx - 3, cy + 1, cx - 2, cy + 2, c);
            Gui.drawRect(cx - 2, cy + 2, cx - 1, cy + 3, c);
            Gui.drawRect(cx - 1, cy + 1, cx, cy + 2, c);
            Gui.drawRect(cx, cy, cx + 1, cy + 1, c);
            Gui.drawRect(cx + 1, cy - 1, cx + 2, cy, c);
            Gui.drawRect(cx + 2, cy - 2, cx + 3, cy - 1, c);
            Gui.drawRect(cx + 3, cy - 3, cx + 4, cy - 2, c);
        } else { // pixel X
            for (int i = -3; i <= 3; i++) {
                Gui.drawRect(cx + i, cy + i, cx + i + 1, cy + i + 1, c);
                Gui.drawRect(cx + i, cy - i, cx + i + 1, cy - i + 1, c);
            }
        }
    }

    /** Segmented bar like the launcher's progress / memory bars. */
    public static void segBar(int x, int y, int w, int h, float fraction, int cells, Style s) {
        well(x, y, x + w, y + h);
        int inner = w - 4;
        int cw = Math.max(2, (inner - (cells - 1)) / cells);
        int on = Math.round(Math.max(0, Math.min(1, fraction)) * cells);
        for (int i = 0; i < cells; i++) {
            int cx = x + 2 + i * (cw + 1);
            if (cx + cw > x + w - 2) break;
            if (i < on) {
                Gui.drawRect(cx, y + 2, cx + cw, y + h - 2, s.fill);
                Gui.drawRect(cx, y + 2, cx + cw, y + 3, s.hi);
                Gui.drawRect(cx, y + h - 3, cx + cw, y + h - 2, s.lo);
            } else {
                Gui.drawRect(cx, y + 2, cx + cw, y + h - 2, PANEL);
            }
        }
    }

    /** Small "rank" style tag. */
    public static void tag(int x, int y, String text, int color) {
        int w = font().getStringWidth(text) + 4;
        Gui.drawRect(x, y, x + w, y + 10, OUTLINE);
        Gui.drawRect(x + 1, y + 1, x + w - 1, y + 9, color);
        font().drawString(text, x + 2, y + 1, CREAM, false);
    }

    private static final String[] GEAR = {
            "   ###   ",
            " # ### # ",
            "  #####  ",
            "###   ###",
            "###   ###",
            "###   ###",
            "  #####  ",
            " # ### # ",
            "   ###   ",
    };

    /** 9x9 pixel gear ("settings") icon. */
    public static void gear(int x, int y, int color) {
        for (int r = 0; r < GEAR.length; r++) {
            String row = GEAR[r];
            int c = 0;
            while (c < row.length()) {
                if (row.charAt(c) != '#') { c++; continue; }
                int start = c;
                while (c < row.length() && row.charAt(c) == '#') c++;
                Gui.drawRect(x + start, y + r, x + c, y + r + 1, color);
            }
        }
    }

    public static void logo(int x, int y, int size) {
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOGO);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }

    /** Dark checker-dithered overlay behind dialogs. */
    public static void overlay(int w, int h) {
        Gui.drawRect(0, 0, w, h, 0xB008050B);
    }

    public static int colorFor(String name) {
        int[] palette = {COPPER, GREEN, BLUE, 0xFFA87BE8, RED, YELLOW, 0xFF2BB5A0, 0xFFE070B0};
        int h = 0;
        for (char c : name.toCharArray()) h = h * 31 + c;
        return palette[Math.abs(h) % palette.length];
    }

    public static int lighten(int c, float f) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r = (int) (r + (255 - r) * f);
        g = (int) (g + (255 - g) * f);
        b = (int) (b + (255 - b) * f);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static int darken(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
