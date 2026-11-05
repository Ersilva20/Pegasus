package dev.mylauncher.core.gui;

import dev.mylauncher.core.Feature;
import dev.mylauncher.core.SettingField;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;

/** Hue / saturation / brightness / alpha sliders plus a hex box. */
public class ColorPickerScreen extends GuiScreen {
    private static final String[] LABELS = {"Hue", "Saturation", "Brightness", "Opacity"};
    private static final int[] PRESETS = {0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF,
            0xFF5555FF, 0xFFFF55FF, 0xFFAAAAAA, 0xFF000000};

    private final GuiScreen parent;
    private final Feature feature;
    private final SettingField field;
    private final float[] hsba = new float[4];
    private int dragging = -1;
    private GuiTextField hex;
    private int px, py;

    public ColorPickerScreen(GuiScreen parent, Feature feature, SettingField field) {
        this.parent = parent;
        this.feature = feature;
        this.field = field;
        load((Integer) field.get());
    }

    private void load(int argb) {
        float[] hsb = Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, null);
        hsba[0] = hsb[0];
        hsba[1] = hsb[1];
        hsba[2] = hsb[2];
        hsba[3] = ((argb >>> 24) & 0xFF) / 255f;
    }

    private int current() {
        int rgb = Color.HSBtoRGB(hsba[0], hsba[1], hsba[2]) & 0xFFFFFF;
        return ((int) (hsba[3] * 255) << 24) | rgb;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        px = width / 2 - 110;
        py = height / 2 - 80;
        hex = new GuiTextField(0, fontRendererObj, px + 124, py + 140, 82, 10);
        hex.setEnableBackgroundDrawing(false);
        hex.setMaxStringLength(9);
        hex.setText(String.format("#%08X", current()));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void apply() {
        field.set(current());
        feature.onSettingsChanged();
        if (!hex.isFocused()) hex.setText(String.format("#%08X", current()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Pixel.overlay(width, height);
        Pixel.window(px, py - 4, px + 220, py + 162, field.meta.name(), null);

        if (dragging >= 0) {
            if (Mouse.isButtonDown(0)) {
                hsba[dragging] = Math.max(0, Math.min(1, (mouseX - (px + 70)) / 140f));
                apply();
            } else {
                dragging = -1;
            }
        }

        for (int i = 0; i < 4; i++) {
            int y = py + 26 + i * 22;
            fontRendererObj.drawString(LABELS[i].toUpperCase(), px + 8, y + 3, Pixel.DIM);
            Pixel.well(px + 68, y, px + 212, y + 14);
            int sx = px + 70;
            for (int s = 0; s < 140; s += 2) {
                float t = s / 140f;
                int c;
                if (i == 0) c = Color.HSBtoRGB(t, 1, 1);
                else if (i == 1) c = Color.HSBtoRGB(hsba[0], t, hsba[2]);
                else if (i == 2) c = Color.HSBtoRGB(hsba[0], hsba[1], t);
                else c = ((int) (t * 255) << 24) | (current() & 0xFFFFFF);
                if (i != 3) c |= 0xFF000000;
                drawRect(sx + s, y + 2, sx + s + 2, y + 12, c);
            }
            int kx = sx + (int) (hsba[i] * 140);
            Pixel.box(kx - 2, y - 1, kx + 3, y + 15, Pixel.CREAM, 0, Pixel.DIM);
        }

        // Preview + presets
        Pixel.well(px + 6, py + 112, px + 114, py + 152);
        drawRect(px + 8, py + 114, px + 60, py + 150, 0xFF000000 | current());
        drawRect(px + 60, py + 114, px + 112, py + 150, current());
        for (int i = 0; i < PRESETS.length; i++) {
            int x = px + 120 + (i % 5) * 18, y = py + 112 + (i / 5) * 10;
            Pixel.box(x, y, x + 16, y + 9, PRESETS[i], 0, 0);
        }
        Pixel.well(px + 120, py + 136, px + 212, py + 152);
        hex.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        hex.mouseClicked(mouseX, mouseY, button);
        for (int i = 0; i < 4; i++) {
            int y = py + 26 + i * 22;
            if (mouseX >= px + 66 && mouseX <= px + 214 && mouseY >= y && mouseY < y + 14) {
                dragging = i;
                return;
            }
        }
        for (int i = 0; i < PRESETS.length; i++) {
            int x = px + 120 + (i % 5) * 18, y = py + 112 + (i / 5) * 10;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 9) {
                float a = hsba[3];
                load(PRESETS[i]);
                hsba[3] = a;
                apply();
                return;
            }
        }
        if (mouseX < px || mouseX > px + 220 || mouseY < py || mouseY > py + 160) mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (hex.textboxKeyTyped(typedChar, keyCode)) {
            String t = hex.getText().replace("#", "").trim();
            try {
                if (t.length() == 6) t = "FF" + t;
                if (t.length() == 8) {
                    load((int) Long.parseLong(t, 16));
                    field.set(current());
                    feature.onSettingsChanged();
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
