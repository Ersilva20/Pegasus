package dev.mylauncher.core.hud;

import dev.mylauncher.core.Category;

import java.util.Collections;
import java.util.List;

/** Convenience base for a one-line text HUD. */
public abstract class SimpleTextHud extends TextHud {
    protected SimpleTextHud(String id, String name, String description, float defaultX, float defaultY) {
        super(id, name, description, Category.HUD, false, defaultX, defaultY);
    }

    /** Text to show, or null to hide. */
    protected abstract String text();

    protected String sample() {
        return name;
    }

    @Override
    protected List<String> lines() {
        String t = text();
        return t == null ? Collections.<String>emptyList() : Collections.singletonList(t);
    }

    @Override
    protected List<String> sampleLines() {
        return Collections.singletonList(sample());
    }
}
