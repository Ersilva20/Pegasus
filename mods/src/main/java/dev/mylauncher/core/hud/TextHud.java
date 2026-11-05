package dev.mylauncher.core.hud;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.Gui;

import java.util.Collections;
import java.util.List;

/** A HUD element made of one or more lines of text, with an optional box background and brackets. */
public abstract class TextHud extends HudFeature {
    @Setting(name = "Text Color", color = true)
    public int textColor = 0xFFFFFFFF;

    @Setting(name = "Text Shadow")
    public boolean textShadow = true;

    @Setting(name = "Background")
    public boolean background = true;

    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0x6F000000;

    @Setting(name = "Brackets")
    public boolean brackets = false;

    @Setting(name = "Fixed Width Box", description = "Use a fixed 56px wide box so short and long values line up")
    public boolean fixedWidth = true;

    @Setting(name = "Padding", min = 0, max = 10, step = 1)
    public int padding = 4;

    protected TextHud(String id, String name, String description, Category category, boolean enabledByDefault,
                      float defaultX, float defaultY) {
        super(id, name, description, category, enabledByDefault, defaultX, defaultY);
    }

    /** The lines to show. Return an empty list to hide (unless editing). */
    protected abstract List<String> lines();

    /** Lines shown in the HUD editor when there is nothing real to show. */
    protected List<String> sampleLines() {
        return Collections.singletonList(name);
    }

    private List<String> current(boolean editing) {
        List<String> l = inGame() ? lines() : Collections.<String>emptyList();
        if ((l == null || l.isEmpty()) && editing) l = sampleLines();
        return l == null ? Collections.<String>emptyList() : l;
    }

    private String decorate(String s) {
        return brackets ? "[" + s + "]" : s;
    }

    @Override
    public boolean shouldRender() {
        List<String> l = lines();
        return l != null && !l.isEmpty();
    }

    @Override
    public int width() {
        int w = 0;
        for (String s : current(true)) w = Math.max(w, mc.fontRendererObj.getStringWidth(decorate(s)));
        w += padding * 2;
        return fixedWidth && current(true).size() == 1 ? Math.max(w, 56) : w;
    }

    @Override
    public int height() {
        int n = Math.max(1, current(true).size());
        return n * 10 - 1 + padding * 2;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        List<String> l = current(editing);
        int w = width(), h = height();
        if (background) Gui.drawRect(0, 0, w, h, backgroundColor);
        int y = padding;
        for (String s : l) {
            String t = decorate(s);
            int x = (w - mc.fontRendererObj.getStringWidth(t)) / 2;
            if (l.size() > 1 && !fixedWidth) x = padding;
            mc.fontRendererObj.drawString(t, x, y, textColor, textShadow);
            y += 10;
        }
    }
}
