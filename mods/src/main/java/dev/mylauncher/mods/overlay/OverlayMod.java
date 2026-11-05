package dev.mylauncher.mods.overlay;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

/** Fire / water / pumpkin / portal / block overlay tweaks. */
@Mod(modid = "ml_overlay", name = "Overlay", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class OverlayMod extends Feature {
    public enum Fire { NORMAL, LOWERED, HIDDEN }

    @Setting(name = "Fire Overlay")
    public Fire fire = Fire.LOWERED;
    @Setting(name = "Fire Height Offset", min = 0, max = 0.6, step = 0.05)
    public float fireOffset = 0.3f;
    @Setting(name = "Hide Water Overlay")
    public boolean water = true;
    @Setting(name = "Hide Suffocation (In-Block) Overlay")
    public boolean block = true;
    @Setting(name = "Hide Pumpkin Blur")
    public boolean pumpkin = true;
    @Setting(name = "Hide Nether Portal Overlay")
    public boolean portal = false;

    private Method renderFire;
    private boolean drawingFire;

    public OverlayMod() {
        super("overlay", "Overlay", "Configure various texture overlays, tweaks, and modifiers.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        switch (event.overlayType) {
            case FIRE:
                if (drawingFire || fire == Fire.NORMAL) return;
                event.setCanceled(true);
                if (fire == Fire.LOWERED) drawLoweredFire(event.renderPartialTicks);
                break;
            case WATER:
                if (water) event.setCanceled(true);
                break;
            case BLOCK:
                if (block) event.setCanceled(true);
                break;
        }
    }

    private void drawLoweredFire(float pt) {
        try {
            if (renderFire == null) {
                renderFire = ReflectionHelper.findMethod(ItemRenderer.class, mc.getItemRenderer(),
                        new String[]{"renderFireInFirstPerson", "func_78442_d"}, float.class);
            }
            drawingFire = true;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -fireOffset, 0);
            renderFire.invoke(mc.getItemRenderer(), pt);
            GlStateManager.popMatrix();
        } catch (Exception e) {
            MLCore.LOG.warn("Lowered fire overlay unavailable", e);
            fire = Fire.HIDDEN;
        } finally {
            drawingFire = false;
        }
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.HELMET && pumpkin) {
            ItemStack head = mc.thePlayer.inventory.armorItemInSlot(3);
            if (head != null && head.getItem() == Item.getItemFromBlock(Blocks.pumpkin)) event.setCanceled(true);
        }
        if (event.type == RenderGameOverlayEvent.ElementType.PORTAL && portal) event.setCanceled(true);
    }
}
