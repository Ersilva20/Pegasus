package dev.mylauncher.core;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hook points called from Pegasus Core's mixins. Mod jars set these while enabled and reset them when
 * disabled. Mixins live only in Pegasus Core so each mod jar stays a plain Forge mod.
 */
public final class Hooks {
    private Hooks() {
    }

    // ---- HUD ----

    public interface OverlayRenderer {
        /** @return true to cancel the vanilla rendering */
        boolean render(GuiIngame gui, int width, int height, float partialTicks);
    }

    public static OverlayRenderer title;
    public static OverlayRenderer actionBar;

    public interface ScoreboardRenderer {
        boolean render(ScoreObjective objective, ScaledResolution resolution);
    }

    public static ScoreboardRenderer scoreboard;

    public interface PingRenderer {
        boolean render(int width, int x, int y, NetworkPlayerInfo info);
    }

    public static PingRenderer tabPing;
    public static float tabScale = 1.0f;

    /** ARGB or {@link Integer#MIN_VALUE} for vanilla chat line background. */
    public static int chatBackground = Integer.MIN_VALUE;
    public static int chatHistory = 100;

    /** Extra offset applied to item tooltips (scrollable tooltips). */
    public static int tooltipOffsetX = 0;
    public static int tooltipOffsetY = 0;

    // ---- Camera / player ----

    public interface PartialTickCancel {
        boolean cancel(float partialTicks);
    }

    public static PartialTickCancel hurtCam;

    public interface AnglesHook {
        /** @return true to stop the player itself rotating (mouse movement already consumed) */
        boolean onTurn(Entity entity, float yaw, float pitch);
    }

    public static AnglesHook turn;

    // ---- Entity / item rendering ----

    /** ARGB hurt overlay color, or 0 for vanilla red. */
    public static int hitColor = 0;

    public interface DroppedItemTransform {
        /** Return a model count to replace vanilla transform (you applied GL transforms), or null for vanilla. */
        Integer transform(EntityItem item, double x, double y, double z, float partialTicks, IBakedModel model);
    }

    public static final List<DroppedItemTransform> droppedItemTransform = new CopyOnWriteArrayList<DroppedItemTransform>();

    public interface DroppedItemScale {
        float scale(EntityItem item);
    }

    public static final List<DroppedItemScale> droppedItemScale = new CopyOnWriteArrayList<DroppedItemScale>();

    public interface FirstPersonTransform {
        /** Called right before vanilla positions the held item; may apply extra GL transforms. */
        void beforeTransform(ItemStack stack, float equipProgress, float swingProgress);

        /** Lets a mod replace the swing progress (e.g. 1.7 block-hit animation). */
        float swing(ItemStack stack, float equipProgress, float vanillaSwing, float realSwing);
    }

    public static final List<FirstPersonTransform> firstPerson = new CopyOnWriteArrayList<FirstPersonTransform>();

    /** Item enchant glint color (ARGB). Vanilla = 0xFF8040CC. */
    public static int itemGlint = -8372020;
    /** Armor glint RGB multiplier, null = vanilla. */
    public static float[] armorGlint = null;

    public interface HitboxRenderer {
        boolean render(Entity entity, double x, double y, double z, float yaw, float partialTicks);
    }

    public static HitboxRenderer hitbox;

    // ---- Text ----

    public interface TextFilter {
        String filter(String text);
    }

    public static final List<TextFilter> textFilters = new CopyOnWriteArrayList<TextFilter>();

    public interface SkinOverride {
        /** Return a replacement skin, or null for the real one. */
        net.minecraft.util.ResourceLocation skin(net.minecraft.client.entity.AbstractClientPlayer player);
    }

    public static SkinOverride skin;

    /** Replacement cape texture (64x32 cape layout, any multiple), or null for the player's real cape. */
    public static SkinOverride cape;

    public interface CapeRenderer {
        /** Draw the player's cape yourself; return true to skip vanilla's rigid cape. */
        boolean render(net.minecraft.client.entity.AbstractClientPlayer player, float partialTicks);
    }

    public static CapeRenderer capeRenderer;

    public static String filterText(String text) {
        if (text == null || textFilters.isEmpty()) return text;
        for (TextFilter f : textFilters) text = f.filter(text);
        return text;
    }

    // ---- Inventory ----

    public interface GuiItemOverlay {
        /** Called after an item (and its glint) is drawn in any GUI or HUD at x,y (16x16). */
        void afterItem(ItemStack stack, int x, int y);
    }

    public static final List<GuiItemOverlay> guiItem = new CopyOnWriteArrayList<GuiItemOverlay>();

    public interface SlotClick {
        /** @return true to cancel the click */
        boolean onClick(GuiContainer gui, Slot slot, int slotId, int button, int clickType);
    }

    public static final List<SlotClick> slotClick = new CopyOnWriteArrayList<SlotClick>();

    public interface DropItem {
        /** @return true to cancel dropping the held item */
        boolean onDrop(ItemStack held, boolean fullStack);
    }

    public static final List<DropItem> dropItem = new CopyOnWriteArrayList<DropItem>();
}
