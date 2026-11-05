package dev.mylauncher.core;

public enum Category {
    HUD("HUD"),
    PVP("PvP"),
    VISUAL("Visual"),
    WORLD("World"),
    INVENTORY("Inventory"),
    CHAT("Chat"),
    UTILITY("Utility"),
    HYPIXEL("Hypixel");

    public final String display;

    Category(String display) {
        this.display = display;
    }
}
