package dev.mylauncher.mods.physics;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Pegasus Cape Physics: capes move as real cloth (any cape: Pegasus capes, official capes, your own PNG capes).
 */
@Mod(modid = "ml_physics", name = "Cape Physics", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PhysicsMod extends Feature implements Hooks.CapeRenderer {
    public enum Quality { LOW, MEDIUM, HIGH }

    @Setting(name = "Physics Strength", description = "Master control: how much the cape reacts to movement and wind (0% = hangs still, 200% = very loose)",
            min = 0, max = 2, step = 0.05)
    public float strength = 1f;
    @Setting(name = "Quality", description = "More points = smoother folds, slightly more CPU")
    public Quality quality = Quality.MEDIUM;
    @Setting(name = "Gravity", min = 0.2, max = 2, step = 0.1)
    public float gravity = 1f;
    @Setting(name = "Movement Sway", description = "How much the cape reacts when you move or turn", min = 0, max = 1.5, step = 0.05)
    public float sway = 1f;
    @Setting(name = "Stiffness", min = 0, max = 8, step = 1)
    public int stiffness = 3;
    @Setting(name = "Wind")
    public boolean wind = true;
    @Setting(name = "Wind Strength", min = 0, max = 3, step = 0.1)
    public float windStrength = 1f;
    @Setting(name = "Body Collision", description = "Keeps the cape from going through your back and legs")
    public boolean collision = true;
    @Setting(name = "Apply to Other Players")
    public boolean others = true;

    private final Map<Integer, ClothCape> capesByEntity = new HashMap<Integer, ClothCape>();
    private int ticks;

    public PhysicsMod() {
        super("physics", "Cape Physics", "Capes move like real cloth instead of a stiff slab. Press P to turn it on/off.", Category.VISUAL, true);
        // Always listening (unlike the feature's own handlers), so the key can also turn the physics back on.
        MinecraftForge.EVENT_BUS.register(new ToggleKey(this));
    }

    /** "Toggle Cape Physics" key (P by default, change it in Options > Controls > Pegasus Mods). */
    public static final class ToggleKey {
        private final PhysicsMod mod;
        private final KeyBinding key = MLCore.key("Toggle Cape Physics", Keyboard.KEY_P);

        ToggleKey(PhysicsMod mod) {
            this.mod = mod;
        }

        @SubscribeEvent
        public void onKey(InputEvent event) {
            if (!key.isPressed() || mc.currentScreen != null) return;
            mod.toggle();
            MLCore.chat("Cape Physics: " + (mod.isEnabled() ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF"));
        }
    }

    @Override
    protected void onEnable() {
        Hooks.capeRenderer = this;
    }

    @Override
    protected void onDisable() {
        if (Hooks.capeRenderer == this) Hooks.capeRenderer = null;
        capesByEntity.clear();
    }

    @Override
    public void onSettingsChanged() {
        capesByEntity.clear(); // rebuild with the new quality
    }

    private int[] gridSize() {
        switch (quality) {
            case LOW:
                return new int[]{4, 9};
            case HIGH:
                return new int[]{9, 21};
            default:
                return new int[]{6, 14};
        }
    }

    @Override
    public boolean render(AbstractClientPlayer player, float partialTicks) {
        if (!others && player != mc.thePlayer) return false;
        if (!player.hasPlayerInfo() || player.isInvisible() || !player.isWearing(EnumPlayerModelParts.CAPE)
                || player.getLocationCape() == null) {
            return true; // vanilla wouldn't draw a cape either
        }
        ClothCape cloth = capesByEntity.get(player.getEntityId());
        if (cloth == null) {
            int[] g = gridSize();
            cloth = new ClothCape(g[0], g[1]);
            capesByEntity.put(player.getEntityId(), cloth);
        }
        // The simulation itself advances in onTick (game ticks), never while drawing: the Wardrobe preview
        // draws the same player with a fake rotation, which must not affect the cloth.
        if (cloth.lastTick == Integer.MIN_VALUE) {
            cloth.tick(player, this, ticks);
            cloth.lastTick = ticks;
        }
        cloth.lastRendered = System.currentTimeMillis();

        mc.getTextureManager().bindTexture(player.getLocationCape());
        GlStateManager.pushMatrix();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableCull();
        cloth.render(partialTicks);
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        return true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (mc.theWorld != null && !mc.isGamePaused()) {
            for (Map.Entry<Integer, ClothCape> e : capesByEntity.entrySet()) {
                net.minecraft.entity.Entity ent = mc.theWorld.getEntityByID(e.getKey());
                if (!(ent instanceof AbstractClientPlayer) || e.getValue().lastTick == Integer.MIN_VALUE) continue;
                e.getValue().tick((AbstractClientPlayer) ent, this, ticks);
                e.getValue().lastTick = ticks;
            }
        }
        if (ticks % 200 == 0) {
            long cutoff = System.currentTimeMillis() - 10000;
            for (Iterator<ClothCape> it = capesByEntity.values().iterator(); it.hasNext(); ) {
                if (it.next().lastRendered < cutoff) it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onWorld(WorldEvent.Load event) {
        capesByEntity.clear();
    }
}
