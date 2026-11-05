package dev.mylauncher.mods.crosshair;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_crosshair", name = "Crosshair", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class CrosshairMod extends Feature {
    public enum Style { CROSS, CROSS_DOT, DOT, T_SHAPE, CIRCLE, SQUARE, X, CUSTOM }

    /** Size of the drawable grid of the CUSTOM style; the middle cell is the screen center. */
    public static final int GRID = 15;
    static final int MID = GRID / 2;
    /** A vanilla-like plus. */
    static final String DEFAULT_PATTERN = encode(rasterPlus());

    @Setting(name = "Style", description = "CUSTOM uses the crosshair you draw in the editor")
    public Style style = Style.CROSS;
    @Setting(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Setting(name = "Enemy Color", description = "Color when aiming at a player or mob", color = true)
    public int enemyColor = 0xFFFF5555;
    @Setting(name = "Use Enemy Color")
    public boolean useEnemyColor = true;
    @Setting(name = "Size", min = 1, max = 15, step = 1)
    public int size = 5;
    @Setting(name = "Gap", min = 0, max = 10, step = 1)
    public int gap = 2;
    @Setting(name = "Thickness", min = 1, max = 5, step = 1)
    public int thickness = 1;
    @Setting(name = "Outline")
    public boolean outline = true;
    @Setting(name = "Outline Color", color = true)
    public int outlineColor = 0xFF000000;
    @Setting(name = "Show in Third Person")
    public boolean thirdPerson = false;
    @Setting(name = "Show in Debug Screen")
    public boolean debug = false;
    @Setting(name = "Custom Pattern", hidden = true)
    public String pattern = DEFAULT_PATTERN;

    private String parsedFrom;
    private boolean[][] parsed;
    /** When set, draw() marks grid cells instead of drawing (used to turn a style into a pattern). */
    private boolean[][] raster;

    public CrosshairMod() {
        super("crosshair", "Crosshair", "Replace the default Minecraft crosshair with a customizable crosshair.", Category.VISUAL);
    }

    @Override
    public GuiScreen customScreen(GuiScreen parent) {
        return new CrosshairEditorScreen(parent, this);
    }

    private boolean aimingAtEnemy() {
        MovingObjectPosition mop = mc.objectMouseOver;
        return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && (mop.entityHit instanceof EntityLivingBase || mop.entityHit instanceof EntityPlayer);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        event.setCanceled(true);
        if (mc.gameSettings.thirdPersonView != 0 && !thirdPerson) return;
        if (mc.gameSettings.showDebugInfo && !debug) return;
        int cx = event.resolution.getScaledWidth() / 2, cy = event.resolution.getScaledHeight() / 2;
        render(cx, cy, useEnemyColor && aimingAtEnemy() ? enemyColor : color);
    }

    /** Draws the crosshair centered on the pixel at (cx, cy). Also used by the editor preview. */
    public void render(int cx, int cy, int c) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (outline) draw(cx, cy, outlineColor, 1);
        draw(cx, cy, c, 0);
        GlStateManager.color(1, 1, 1, 1);
    }

    // ---------------------------------------------------------------- custom pattern

    /** The CUSTOM pattern as [y][x] cells. */
    public boolean[][] cells() {
        if (!pattern.equals(parsedFrom)) {
            parsed = decode(pattern);
            parsedFrom = pattern;
        }
        return parsed;
    }

    public void setCells(boolean[][] cells) {
        pattern = encode(cells);
    }

    /** One 4-digit hex number per row, bit x = column x. */
    static String encode(boolean[][] cells) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < GRID; y++) {
            int bits = 0;
            for (int x = 0; x < GRID; x++) if (cells[y][x]) bits |= 1 << x;
            if (y > 0) sb.append(',');
            sb.append(String.format("%04x", bits));
        }
        return sb.toString();
    }

    static boolean[][] decode(String s) {
        boolean[][] cells = new boolean[GRID][GRID];
        String[] rows = s == null ? new String[0] : s.split(",");
        for (int y = 0; y < GRID && y < rows.length; y++) {
            int bits;
            try {
                bits = Integer.parseInt(rows[y].trim(), 16);
            } catch (NumberFormatException e) {
                continue;
            }
            for (int x = 0; x < GRID; x++) cells[y][x] = (bits & (1 << x)) != 0;
        }
        return cells;
    }

    private static boolean[][] rasterPlus() {
        boolean[][] c = new boolean[GRID][GRID];
        for (int i = MID - 4; i <= MID + 4; i++) {
            c[MID][i] = true;
            c[i][MID] = true;
        }
        return c;
    }

    /** The given built-in style (with the current size/gap/thickness) as a pattern, for the editor's presets. */
    public boolean[][] rasterize(Style s) {
        Style old = style;
        raster = new boolean[GRID][GRID];
        try {
            style = s;
            draw(MID, MID, 0, 0);
            return raster;
        } finally {
            style = old;
            raster = null;
        }
    }

    // ---------------------------------------------------------------- drawing

    private void bar(int l, int t, int r, int b, int c, int grow) {
        if (raster != null) {
            for (int y = Math.max(0, t); y < Math.min(GRID, b); y++)
                for (int x = Math.max(0, l); x < Math.min(GRID, r); x++) raster[y][x] = true;
            return;
        }
        Gui.drawRect(l - grow, t - grow, r + grow, b + grow, c);
    }

    private void drawCustom(int cx, int cy, int c, int o) {
        boolean[][] cells = cells();
        for (int y = 0; y < GRID; y++) {
            int x = 0;
            while (x < GRID) { // one rect per horizontal run of cells
                if (!cells[y][x]) { x++; continue; }
                int start = x;
                while (x < GRID && cells[y][x]) x++;
                bar(cx - MID + start, cy - MID + y, cx - MID + x, cy - MID + y + 1, c, o);
            }
        }
    }

    private void draw(int cx, int cy, int c, int o) {
        int th = thickness, half = th / 2;
        int l = cx - half, t = cy - half;
        switch (style) {
            case CUSTOM:
                drawCustom(cx, cy, c, o);
                break;
            case DOT:
                bar(l, t, l + th, t + th, c, o);
                break;
            case CIRCLE: {
                int r = size + gap;
                for (int a = 0; a < 360; a += 10) {
                    double rad = Math.toRadians(a);
                    int x = (int) Math.round(cx + Math.cos(rad) * r), y = (int) Math.round(cy + Math.sin(rad) * r);
                    bar(x - half, y - half, x - half + th, y - half + th, c, o);
                }
                break;
            }
            case SQUARE: {
                int x1 = cx - size, y1 = cy - size, x2 = cx + size + 1, y2 = cy + size + 1;
                bar(x1, y1, x2, y1 + 1, c, o);
                bar(x1, y2 - 1, x2, y2, c, o);
                bar(x1, y1, x1 + 1, y2, c, o);
                bar(x2 - 1, y1, x2, y2, c, o);
                break;
            }
            case X:
                for (int i = gap; i <= gap + size; i++) {
                    bar(cx - i, cy - i, cx - i + th, cy - i + th, c, o);
                    bar(cx + i, cy - i, cx + i + th, cy - i + th, c, o);
                    bar(cx - i, cy + i, cx - i + th, cy + i + th, c, o);
                    bar(cx + i, cy + i, cx + i + th, cy + i + th, c, o);
                }
                break;
            default: {
                boolean top = style != Style.T_SHAPE;
                bar(l - gap - size, t, l - gap, t + th, c, o);
                bar(l + th + gap, t, l + th + gap + size, t + th, c, o);
                if (top) bar(l, t - gap - size, l + th, t - gap, c, o);
                bar(l, t + th + gap, l + th, t + th + gap + size, c, o);
                if (style == Style.CROSS_DOT) bar(l, t, l + th, t + th, c, o);
            }
        }
    }
}
