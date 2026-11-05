package dev.mylauncher.core.gui;

import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.hud.HudRenderer;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Drag HUD elements, scroll to resize, right-click for settings. Snaps to screen center/edges. */
public class HudEditorScreen extends GuiScreen {
    private static final int SNAP = 4;

    private final GuiScreen parent;
    private HudFeature dragging;
    private float grabX, grabY;
    private boolean snapX, snapY;

    public HudEditorScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private List<HudFeature> huds() {
        List<HudFeature> out = new ArrayList<HudFeature>();
        for (Feature f : MLCore.features()) if (f instanceof HudFeature && f.isEnabled()) out.add((HudFeature) f);
        return out;
    }

    private HudFeature at(ScaledResolution sr, int mx, int my) {
        List<HudFeature> list = huds();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudFeature h = list.get(i);
            int x = h.screenX(sr), y = h.screenY(sr);
            if (mx >= x && my >= y && mx <= x + h.width() * h.scale && my <= y + h.height() * h.scale) return h;
        }
        return null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        drawRect(0, 0, width, height, 0x40000000);
        snapX = snapY = false;

        if (dragging != null) {
            if (!Mouse.isButtonDown(0)) {
                Config.save(dragging);
                dragging = null;
            } else {
                float w = dragging.width() * dragging.scale, h = dragging.height() * dragging.scale;
                float nx = mouseX - grabX, ny = mouseY - grabY;
                if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    float cx = nx + w / 2, cy = ny + h / 2;
                    if (Math.abs(cx - width / 2f) < SNAP) {
                        nx = width / 2f - w / 2;
                        snapX = true;
                    }
                    if (Math.abs(cy - height / 2f) < SNAP) {
                        ny = height / 2f - h / 2;
                        snapY = true;
                    }
                    if (nx < SNAP) nx = 0;
                    if (ny < SNAP) ny = 0;
                    if (width - (nx + w) < SNAP) nx = width - w;
                    if (height - (ny + h) < SNAP) ny = height - h;
                }
                dragging.setScreenPos(sr, nx, ny);
            }
        }
        if (snapX) drawRect(width / 2, 0, width / 2 + 1, height, 0xC0D9823B);
        if (snapY) drawRect(0, height / 2, width, height / 2 + 1, 0xC0D9823B);

        HudRenderer.renderAll(sr, partialTicks, true);

        HudFeature hover = dragging != null ? dragging : at(sr, mouseX, mouseY);
        for (HudFeature h : huds()) {
            int x = h.screenX(sr), y = h.screenY(sr);
            int r = x + Math.round(h.width() * h.scale), b = y + Math.round(h.height() * h.scale);
            RenderUtil.outline(x - 1, y - 1, r + 1, b + 1, h == hover ? Pixel.COPPER : 0x70F3E9D2);
        }
        if (hover != null) {
            String tip = hover.name + "  (" + Math.round(hover.scale * 100) + "%)";
            int tx = Math.min(hover.screenX(sr), width - fontRendererObj.getStringWidth(tip) - 2);
            int ty = hover.screenY(sr) > 12 ? hover.screenY(sr) - 11 : hover.screenY(sr) + Math.round(hover.height() * hover.scale) + 3;
            Pixel.tag(tx - 2, ty - 1, tip, Pixel.COPPER_LO);
        }

        // Center "Mods" button + help
        int bx = width / 2 - 40, by = height / 2 - 10;
        Pixel.button(bx, by, 80, 20, "MOD MENU", Pixel.Style.COPPER, dragging == null ? mouseX : -1, mouseY);
        String help = "DRAG TO MOVE • SCROLL TO RESIZE • RIGHT-CLICK FOR SETTINGS • CTRL = NO SNAP";
        int hw = fontRendererObj.getStringWidth(help) + 12;
        Pixel.panel(width / 2 - hw / 2, height - 18, width / 2 + hw / 2, height - 2);
        drawCenteredString(fontRendererObj, help, width / 2, height - 13, Pixel.DIM);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        HudFeature h = at(sr, mouseX, mouseY);
        if (h == null) {
            int bx = width / 2 - 40, by = height / 2 - 10;
            if (mouseX >= bx && mouseX < bx + 80 && mouseY >= by && mouseY < by + 20) {
                mc.displayGuiScreen(parent instanceof ModMenuScreen ? parent : new ModMenuScreen());
            }
            return;
        }
        if (button == 1) {
            mc.displayGuiScreen(new SettingsScreen(this, h));
            return;
        }
        if (button == 0) {
            dragging = h;
            grabX = mouseX - h.screenX(sr);
            grabY = mouseY - h.screenY(sr);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        HudFeature h = at(sr, mx, my);
        if (h == null) return;
        int x = h.screenX(sr), y = h.screenY(sr);
        h.scale = Math.max(0.5f, Math.min(3f, Math.round((h.scale + Integer.signum(wheel) * 0.05f) * 20) / 20f));
        h.setScreenPos(sr, x, y);
        Config.save(h);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
    }

    @Override
    public void onGuiClosed() {
        for (HudFeature h : huds()) Config.save(h);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
