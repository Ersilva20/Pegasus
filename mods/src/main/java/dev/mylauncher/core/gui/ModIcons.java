package dev.mylauncher.core.gui;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** A Minecraft item used as the icon of each mod in the mod menu (falls back to one per category). */
public final class ModIcons {
    private ModIcons() {
    }

    private static Map<String, ItemStack> icons;

    private static void put(String id, Item item) {
        put(id, item, 0);
    }

    private static void put(String id, Item item, int meta) {
        icons.put(id, new ItemStack(item, 1, meta));
    }

    private static void put(String id, Block block) {
        put(id, block, 0);
    }

    private static void put(String id, Block block, int meta) {
        icons.put(id, new ItemStack(block, 1, meta));
    }

    private static void build() {
        icons = new HashMap<String, ItemStack>();
        // HUD
        put("fps", Items.comparator);
        put("cps", Blocks.stone_button);
        put("memory", Blocks.redstone_block);
        put("ping", Items.glowstone_dust);
        put("coordinates", Items.map);
        put("directionhud", Items.compass);
        put("clock", Items.clock);
        put("serveraddress", Items.nether_star);
        put("daycounter", Blocks.double_plant); // sunflower
        put("reachdisplay", Items.bow);
        put("combo", Items.blaze_rod);
        put("momentum", Items.feather);
        put("viewangles", Items.arrow);
        put("playtime", Items.cake);
        put("stopwatch", Blocks.redstone_lamp);
        put("packdisplay", Items.painting);
        put("tps", Items.repeater);
        put("potioncounter", Items.potionitem, 16421);
        put("itemcounter", Items.golden_apple);
        put("armorstatus", Items.diamond_chestplate);
        put("potioneffects", Items.brewing_stand);
        put("keystrokes", Blocks.wooden_button);
        put("inventoryhud", Blocks.chest);
        put("waila", Items.spider_eye);
        put("hotbarkeys", Blocks.lever);
        put("saturation", Items.cooked_beef);
        put("itemtracker", Items.diamond);
        put("scoreboard", Items.sign);
        put("tabeditor", Items.name_tag);
        put("titles", Items.paper);
        put("actionbar", Items.writable_book);
        put("bossbar", Items.skull, 1);
        put("f3display", Blocks.command_block);
        put("hotbarcustomizer", Blocks.trapdoor);
        put("minimap", Items.filled_map);
        put("togglesneak", Items.leather_boots);
        // PvP
        put("pvpinfo", Items.diamond_sword);
        put("knockbacktrainer", Items.stick);
        put("teamview", Items.banner, 4);
        put("cooldowns", Items.ender_pearl);
        put("hurtcam", Items.rotten_flesh);
        put("damagetint", Items.redstone);
        put("hitcolor", Items.dye, 1);
        put("hitbox", Blocks.glass);
        put("oldvisuals", Items.golden_sword);
        put("uhcoverlay", Items.golden_apple, 1);
        put("markers", Items.banner, 1);
        put("killsounds", Items.record_13);
        // Visual
        put("crosshair", Items.fishing_rod);
        put("zoom", Items.ender_eye);
        put("fov", Blocks.glass_pane);
        put("freelook", Items.skull, 3);
        put("snaplook", Blocks.pumpkin);
        put("mobsize", Items.spawn_egg, 50);
        put("blockoutline", Blocks.grass);
        put("lighting", Blocks.torch);
        put("overlay", Blocks.stained_glass, 14);
        put("particlechanger", Items.blaze_powder);
        put("glintcolorizer", Items.enchanted_book);
        put("shinypots", Items.experience_bottle);
        put("itemphysics", Items.slime_ball);
        put("items2d", Items.apple);
        put("itemcustomizer", Items.iron_sword);
        put("nametags", Items.name_tag);
        put("motionblur", Items.sugar);
        put("menublur", Blocks.ice);
        put("colorsaturation", Items.dye, 13);
        put("physics", Blocks.wool, 14);
        // World
        put("timechanger", Blocks.daylight_detector);
        put("weatherchanger", Items.water_bucket);
        put("fog", Blocks.web);
        put("chunkborders", Blocks.bedrock);
        put("tntcountdown", Blocks.tnt);
        put("heightlimit", Blocks.ladder);
        put("lightoverlay", Blocks.redstone_torch);
        put("waypoints", Blocks.beacon);
        put("customskies", Blocks.wool, 3);
        put("worldeditcui", Items.wooden_axe);
        // Inventory
        put("inventorymod", Blocks.ender_chest);
        put("slotlocking", Blocks.iron_bars);
        put("dropprotection", Blocks.barrier);
        put("slotbinding", Items.lead);
        put("inventorysearch", Blocks.hopper);
        put("containeranimations", Blocks.trapped_chest);
        put("scrolltooltips", Items.written_book);
        // Chat
        put("chat", Items.book);
        put("autotexthotkey", Items.feather);
        put("autotextactions", Items.writable_book);
        // Hypixel
        put("quickplay", Items.fireworks);
        put("hypixelmods", Items.gold_ingot);
        put("bedwars", Items.bed);
        put("hypixelstats", Items.emerald);
        // Utility
        put("nickhider", Items.leather_helmet);
        put("guiscale", Items.item_frame);
        put("packorganizer", Blocks.bookshelf);
        put("bettersounds", Blocks.noteblock);
        put("screenshot", Items.painting);
        put("mumblelink", Items.record_cat);
        put("mediaplayer", Blocks.jukebox);
        put("horsestats", Items.saddle);
    }

    private static ItemStack forCategory(Category c) {
        switch (c) {
            case HUD: return new ItemStack(Items.painting);
            case PVP: return new ItemStack(Items.iron_sword);
            case VISUAL: return new ItemStack(Items.ender_eye);
            case WORLD: return new ItemStack(Blocks.grass);
            case INVENTORY: return new ItemStack(Blocks.chest);
            case CHAT: return new ItemStack(Items.book);
            case HYPIXEL: return new ItemStack(Items.gold_ingot);
            default: return new ItemStack(Items.redstone);
        }
    }

    public static ItemStack of(Feature f) {
        if (icons == null) build();
        ItemStack s = icons.get(f.id);
        return s != null ? s : forCategory(f.category);
    }

    /** Draws the mod's icon with its top-left at (x, y), {@code size} GUI pixels wide (16 = normal item size). */
    public static void draw(Feature f, int x, int y, int size) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        float s = size / 16f;
        GlStateManager.scale(s, s, 1);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableDepth();
        try {
            Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(of(f), 0, 0);
        } catch (Exception ignored) {
            // a broken item model must never break the menu
        }
        GlStateManager.disableDepth();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }
}
