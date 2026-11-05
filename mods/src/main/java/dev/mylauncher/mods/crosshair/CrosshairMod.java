package dev.mylauncher.mods.crosshair;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_crosshair", name = "Crosshair", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class CrosshairMod extends Feature {
    public enum Style { CROSS, CROSS_DOT, DOT, T_SHAPE, CIRCLE, SQUARE, X }

    @Setting(name = "Style")
    public Style style = Style.CROSS;
    @Setting(name = "Color", color = true)
    public int color = 0xFFFFFFFF;
    @Setting(name = "Enemy Color", description = "Color when aiming at a player or mob", color = true)
    public int enemyColor = 0xFFFF5555;
    @Setting(name = "Use Enemy Color")
    public boolean useEnemyColor = true;
    @Setting(name = "Size", min = 1, max = 15, step = 1)
    public int size = 5;
    @Setting(name = "Gap", min = 0, max = 10, step = 1)
    public int gap = 2;
    @Setting(name = "Thickness", min = 1, max = 5, step = 1)
    public int thickness = 1;
    @Setting(name = "Outline")
    public boolean outline = true;
    @Setting(name = "Outline Color", color = true)
    public int outlineColor = 0xFF000000;
    @Setting(name = "Show in Third Person")
    public boolean thirdPerson = false;
    @Setting(name = "Show in Debug Screen")
    public boolean debug = false;

    public CrosshairMod() {
        super("crosshair", "Crosshair", "Replace the default Minecraft crosshair with a customizable crosshair.", Category.VISUAL);
    }

    private boolean aimingAtEnemy() {
        MovingObjectPosition mop = mc.objectMouseOver;
        return mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                && (mop.entityHit instanceof EntityLivingBase || mop.entityHit instanceof EntityPlayer);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        event.setCanceled(true);
        if (mc.gameSettings.thirdPersonView != 0 && !thirdPerson) return;
        if (mc.gameSettings.showDebugInfo && !debug) return;
        int cx = event.resolution.getScaledWidth() / 2, cy = event.resolution.getScaledHeight() / 2;
        int c = useEnemyColor && aimingAtEnemy() ? enemyColor : color;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (outline) draw(cx, cy, outlineColor, 1);
        draw(cx, cy, c, 0);
        GlStateManager.color(1, 1, 1, 1);
    }

    private void bar(int l, int t, int r, int b, int c, int grow) {
        Gui.drawRect(l - grow, t - grow, r + grow, b + grow, c);
    }

    private void draw(int cx, int cy, int c, int o) {
        int th = thickness, half = th / 2;
        int l = cx - half, t = cy - half;
        switch (style) {
            case DOT:
                bar(l, t, l + th, t + th, c, o);
                break;
            case CIRCLE: {
                int r = size + gap;
                for (int a = 0; a < 360; a += 10) {
                    double rad = Math.toRadians(a);
                    int x = (int) Math.round(cx + Math.cos(rad) * r), y = (int) Math.round(cy + Math.sin(rad) * r);
                    bar(x - half, y - half, x - half + th, y - half + th, c, o);
                }
                break;
            }
            case SQUARE:
                RenderUtil.outline(cx - size - o, cy - size - o, cx + size + 1 + o, cy + size + 1 + o, c);
                if (o == 0) RenderUtil.outline(cx - size, cy - size, cx + size + 1, cy + size + 1, c);
                break;
            case X:
                for (int i = gap; i <= gap + size; i++) {
                    bar(cx - i, cy - i, cx - i + th, cy - i + th, c, o);
                    bar(cx + i, cy - i, cx + i + th, cy - i + th, c, o);
                    bar(cx - i, cy + i, cx - i + th, cy + i + th, c, o);
                    bar(cx + i, cy + i, cx + i + th, cy + i + th, c, o);
                }
                break;
            default: {
                boolean top = style != Style.T_SHAPE;
                bar(l - gap - size, t, l - gap, t + th, c, o);
                bar(l + th + gap, t, l + th + gap + size, t + th, c, o);
                if (top) bar(l, t - gap - size, l + th, t - gap, c, o);
                bar(l, t + th + gap, l + th, t + th + gap + size, c, o);
                if (style == Style.CROSS_DOT) bar(l, t, l + th, t + th, c, o);
            }
        }
    }
}
