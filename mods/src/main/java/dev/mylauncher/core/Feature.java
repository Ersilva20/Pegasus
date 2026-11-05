package dev.mylauncher.core;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * One toggleable mod feature. While enabled, the feature is registered on the Forge event bus,
 * so its {@code @SubscribeEvent} methods only run when the player has it turned on.
 */
public abstract class Feature {
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final boolean enabledByDefault;

    private boolean enabled;
    private List<SettingField> settings;

    protected Feature(String id, String name, String description, Category category, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
    }

    protected Feature(String id, String name, String description, Category category) {
        this(id, name, description, category, false);
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) {
            MinecraftForge.EVENT_BUS.register(this);
            onEnable();
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
            onDisable();
        }
    }

    public final void toggle() {
        if (builtIn()) return;
        setEnabled(!enabled);
        Config.save(this);
    }

    public List<SettingField> settings() {
        if (settings == null) settings = SettingField.scan(this);
        return settings;
    }

    private final java.util.Map<String, Object> defaults = new java.util.HashMap<String, Object>();

    /** Remembers the code defaults so "Reset" in the settings screen can restore them. */
    final void captureDefaults() {
        for (SettingField sf : settings()) defaults.put(sf.key(), sf.get());
    }

    public void resetToDefaults() {
        for (SettingField sf : settings()) {
            if (defaults.containsKey(sf.key())) sf.set(defaults.get(sf.key()));
        }
        onSettingsChanged();
        Config.save(this);
    }

    /** Called when the feature is turned on (also at startup if it was saved as on). */
    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Built-in parts of Pegasus (Cosmetics, Settings): always on and not listed as mods. */
    public boolean builtIn() {
        return false;
    }

    /** A mod can return its own screen (e.g. the Wardrobe) to open instead of the generic settings screen. */
    public net.minecraft.client.gui.GuiScreen customScreen(net.minecraft.client.gui.GuiScreen parent) {
        return null;
    }

    /** Called after any setting changed in the settings screen. */
    public void onSettingsChanged() {
    }

    protected boolean inGame() {
        return mc.thePlayer != null && mc.theWorld != null;
    }
}
