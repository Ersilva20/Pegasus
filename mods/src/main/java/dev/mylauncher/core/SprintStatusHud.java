package dev.mylauncher.core;

import dev.mylauncher.core.hud.SimpleTextHud;

/** The "[Sprinting (Toggled)]" text for Settings > Toggle Sprint / Toggle Sneak. Built in; moved in Edit HUD. */
public class SprintStatusHud extends SimpleTextHud {
    private final ClientSettings settings;

    public SprintStatusHud(ClientSettings settings) {
        // Same id as the old Toggle Sneak mod, so its HUD position carries over.
        super("togglesneak", "Sprint Status", "Sprint / sneak status text", 0f, 0.96f);
        this.settings = settings;
    }

    @Override
    public boolean builtIn() {
        return true;
    }

    @Override
    public boolean shouldRender() {
        return settings.sprintStatus && (settings.toggleSprint || settings.toggleSneak) && super.shouldRender();
    }

    @Override
    protected String text() {
        if (mc.thePlayer.capabilities.isFlying) return "[Flying]";
        if (mc.thePlayer.isSneaking()) return settings.sneaking ? "[Sneaking (Toggled)]" : "[Sneaking (Key Held)]";
        if (mc.thePlayer.isSprinting()) return settings.sprinting ? "[Sprinting (Toggled)]" : "[Sprinting (Vanilla)]";
        return null;
    }

    @Override
    protected String sample() {
        return "[Sprinting (Toggled)]";
    }
}
