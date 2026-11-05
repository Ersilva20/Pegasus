package dev.mylauncher.core.gui;

import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.SettingField;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Settings for one feature, built from its {@code @Setting} fields, in the Pegasus pixel style. */
public class SettingsScreen extends GuiScreen {
    private static final int ROW = 22;
    private static final int WIDGET_W = 110;

    private final GuiScreen parent;
    private final Feature feature;
    private final Map<SettingField, GuiTextField> textFields = new HashMap<SettingField, GuiTextField>();
    private SettingField dragging;
    private int scroll;
    private int px, py, pw, ph;

    public SettingsScreen(GuiScreen parent, Feature feature) {
        this.parent = parent;
        this.feature = feature;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(width - 16, 370);
        ph = Math.min(height - 16, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        textFields.clear();
        for (SettingField sf : feature.settings()) {
            if (sf.type == SettingField.Type.STRING) {
                GuiTextField tf = new GuiTextField(textFields.size() + 1, fontRendererObj, 0, 0, WIDGET_W - 8, 10);
                tf.setEnableBackgroundDrawing(false);
                tf.setMaxStringLength(256);
                tf.setText((String) sf.get());
                textFields.put(sf, tf);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Config.save(feature);
    }

    private int listTop() {
        return py + 50;
    }

    private int viewH() {
        return py + ph - 6 - listTop();
    }

    /** Settings with a section title row (a String) before each setting that starts a section. */
    private List<Object> rows() {
        List<Object> out = new ArrayList<Object>();
        for (SettingField sf : feature.settings()) {
            if (!sf.meta.section().isEmpty()) out.add(sf.meta.section());
            out.add(sf);
        }
        return out;
    }

    private int resetX() {
        return feature.builtIn() ? px + pw - 54 : px + pw - 128;
    }

    private int widgetX() {
        return px + pw - WIDGET_W - 10;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Pixel.overlay(width, height);
        Pixel.window(px, py, px + pw, py + ph, feature.name, feature.builtIn() ? "PEGASUS" : feature.category.display.toUpperCase());

        Pixel.button(px + 6, py + 21, 48, 16, "< BACK", Pixel.Style.GRAY, mouseX, mouseY);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(feature.description, pw - 190), px + 60, py + 25, Pixel.DIM);
        if (!feature.builtIn()) {
            Pixel.button(px + pw - 76, py + 21, 70, 16, feature.isEnabled() ? "ENABLED" : "DISABLED",
                    feature.isEnabled() ? Pixel.Style.GREEN : Pixel.Style.RED, mouseX, mouseY);
        }
        Pixel.button(resetX(), py + 21, 48, 16, "RESET", Pixel.Style.COPPER, mouseX, mouseY);
        drawRect(px + 6, py + 43, px + pw - 6, py + 44, Pixel.OUTLINE);

        List<Object> list = rows();
        int maxScroll = Math.max(0, list.size() * ROW - viewH());
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (dragging != null) {
            if (Mouse.isButtonDown(0)) setSliderFromMouse(dragging, mouseX);
            else dragging = null;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px * f, mc.displayHeight - (listTop() + viewH()) * f, pw * f, viewH() * f);
        int y = listTop() - scroll;
        for (Object row : list) {
            if (y + ROW >= listTop() && y <= listTop() + viewH()) {
                if (row instanceof SettingField) drawRow((SettingField) row, y, mouseX, mouseY);
                else drawSection((String) row, y);
            }
            y += ROW;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (list.isEmpty()) drawCenteredString(fontRendererObj, "THIS MOD HAS NO SETTINGS", px + pw / 2, listTop() + 20, Pixel.DIM);

        if (mouseY >= listTop() && mouseY < listTop() + viewH()) {
            int idx = (mouseY - listTop() + scroll) / ROW;
            if (idx >= 0 && idx < list.size() && list.get(idx) instanceof SettingField && mouseX < widgetX() - 40 && mouseX > px) {
                String d = ((SettingField) list.get(idx)).meta.description();
                if (!d.isEmpty()) drawHoveringText(Collections.singletonList(d), mouseX, mouseY);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSection(String title, int y) {
        fontRendererObj.drawString(title, px + 10, y + 9, Pixel.COPPER);
        int lx = px + 16 + fontRendererObj.getStringWidth(title);
        drawRect(lx, y + 12, px + pw - 8, y + 13, Pixel.PANEL_HI);
    }

    private void drawRow(SettingField sf, int y, int mx, int my) {
        Pixel.box(px + 6, y, px + pw - 6, y + ROW - 2, Pixel.PANEL_LO, 0xFF2C2436, 0);
        fontRendererObj.drawString(sf.meta.name().toUpperCase(), px + 12, y + 6, Pixel.CREAM);
        int wx = widgetX(), wy = y + 2;
        switch (sf.type) {
            case BOOLEAN:
                Pixel.toggle(wx + WIDGET_W - 16, wy, 16, (Boolean) sf.get(), false);
                break;
            case INT:
            case FLOAT:
            case DOUBLE: {
                double min = sf.meta.min(), max = sf.meta.max();
                float pct = (float) ((sf.getNumber() - min) / (max - min));
                Pixel.segBar(wx, wy + 2, WIDGET_W, 12, pct, 14, Pixel.Style.BLUE);
                String v = sf.display();
                fontRendererObj.drawString(v, wx - fontRendererObj.getStringWidth(v) - 6, wy + 4, Pixel.YELLOW);
                break;
            }
            case ENUM:
                Pixel.button(wx, wy, WIDGET_W, 16, "< " + sf.display().toUpperCase() + " >", Pixel.Style.GRAY, mx, my);
                break;
            case COLOR: {
                int c = (Integer) sf.get();
                Pixel.well(wx, wy, wx + WIDGET_W, wy + 16);
                drawRect(wx + 3, wy + 3, wx + 25, wy + 13, 0xFF000000 | c);
                fontRendererObj.drawString(sf.display(), wx + 30, wy + 4, Pixel.CREAM);
                break;
            }
            case STRING: {
                Pixel.well(wx, wy, wx + WIDGET_W, wy + 16);
                GuiTextField tf = textFields.get(sf);
                tf.xPosition = wx + 4;
                tf.yPosition = wy + 4;
                tf.drawTextBox();
                break;
            }
        }
    }

    private void setSliderFromMouse(SettingField sf, int mouseX) {
        double pct = Math.max(0, Math.min(1, (mouseX - widgetX()) / (double) WIDGET_W));
        sf.setNumber(sf.meta.min() + pct * (sf.meta.max() - sf.meta.min()));
        feature.onSettingsChanged();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (hit(mouseX, mouseY, px + 6, py + 21, 48, 16)) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (!feature.builtIn() && hit(mouseX, mouseY, px + pw - 76, py + 21, 70, 16)) {
            feature.toggle();
            return;
        }
        if (hit(mouseX, mouseY, resetX(), py + 21, 48, 16)) {
            feature.resetToDefaults();
            initGui();
            return;
        }
        for (GuiTextField tf : textFields.values()) {
            boolean inside = mouseX >= tf.xPosition - 4 && mouseX < tf.xPosition - 4 + WIDGET_W && mouseY >= tf.yPosition - 4 && mouseY < tf.yPosition + 12;
            tf.setFocused(inside);
            if (inside) tf.mouseClicked(mouseX, mouseY, button);
        }
        if (mouseY < listTop() || mouseY >= listTop() + viewH()) return;
        int idx = (mouseY - listTop() + scroll) / ROW;
        List<Object> list = rows();
        if (idx < 0 || idx >= list.size() || !(list.get(idx) instanceof SettingField)) return;
        SettingField sf = (SettingField) list.get(idx);
        int wx = widgetX();
        if (mouseX < wx - 4) {
            if (sf.type == SettingField.Type.BOOLEAN) {
                sf.set(!(Boolean) sf.get());
                feature.onSettingsChanged();
            }
            return;
        }
        switch (sf.type) {
            case BOOLEAN:
                sf.set(!(Boolean) sf.get());
                break;
            case INT:
            case FLOAT:
            case DOUBLE:
                dragging = sf;
                setSliderFromMouse(sf, mouseX);
                break;
            case ENUM:
                sf.cycleEnum(button == 1 ? -1 : 1);
                break;
            case COLOR:
                mc.displayGuiScreen(new ColorPickerScreen(this, feature, sf));
                return;
            default:
                break;
        }
        feature.onSettingsChanged();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (Map.Entry<SettingField, GuiTextField> e : textFields.entrySet()) {
            if (e.getValue().isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                    e.getValue().setFocused(false);
                    return;
                }
                e.getValue().textboxKeyTyped(typedChar, keyCode);
                e.getKey().set(e.getValue().getText());
                feature.onSettingsChanged();
                return;
            }
        }
        if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll -= Integer.signum(wheel) * ROW;
    }

    @Override
    public void updateScreen() {
        for (GuiTextField tf : textFields.values()) tf.updateCursorCounter();
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
