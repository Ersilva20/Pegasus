package dev.mylauncher.core.hud;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.ScaledResolution;

/**
 * A feature that draws something movable on the HUD. Position is stored as a fraction (0..1) of the
 * free screen space, so elements stay in the same corner at any window size / GUI scale.
 */
public abstract class HudFeature extends Feature {
    @Setting(name = "Scale", min = 0.5, max = 3.0, step = 0.05)
    public float scale = 1.0f;

    public float relX;
    public float relY;

    protected HudFeature(String id, String name, String description, Category category, boolean enabledByDefault,
                         float defaultX, float defaultY) {
        super(id, name, description, category, enabledByDefault);
        this.relX = defaultX;
        this.relY = defaultY;
    }

    /** Unscaled width in GUI pixels. */
    public abstract int width();

    /** Unscaled height in GUI pixels. */
    public abstract int height();

    /**
     * Draw at (0,0); the HUD renderer already translated and scaled.
     *
     * @param editing true inside the HUD editor (draw sample content even if nothing to show)
     */
    public abstract void render(float partialTicks, boolean editing);

    /** Return false to skip drawing this frame (e.g. nothing to show). Ignored while editing. */
    public boolean shouldRender() {
        return true;
    }

    public int screenX(ScaledResolution sr) {
        return Math.round(relX * Math.max(0, sr.getScaledWidth() - width() * scale));
    }

    public int screenY(ScaledResolution sr) {
        return Math.round(relY * Math.max(0, sr.getScaledHeight() - height() * scale));
    }

    public void setScreenPos(ScaledResolution sr, float x, float y) {
        float freeW = Math.max(1, sr.getScaledWidth() - width() * scale);
        float freeH = Math.max(1, sr.getScaledHeight() - height() * scale);
        relX = Math.max(0, Math.min(1, x / freeW));
        relY = Math.max(0, Math.min(1, y / freeH));
    }
}
