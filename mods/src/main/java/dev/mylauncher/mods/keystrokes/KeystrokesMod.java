package dev.mylauncher.mods.keystrokes;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod(modid = "ml_keystrokes", name = "Keystrokes", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class KeystrokesMod extends HudFeature {
    private static final int K = 22, G = 2;

    @Setting(name = "Show Mouse Buttons")
    public boolean mouse = true;
    @Setting(name = "Show CPS on Mouse Buttons")
    public boolean cps = true;
    @Setting(name = "Show Space Bar")
    public boolean space = true;
    @Setting(name = "Background Color", color = true)
    public int idleColor = 0x6F000000;
    @Setting(name = "Pressed Color", color = true)
    public int pressedColor = 0xB0FFFFFF;
    @Setting(name = "Text Color", color = true)
    public int textColor = 0xFFFFFFFF;
    @Setting(name = "Pressed Text Color", color = true)
    public int pressedTextColor = 0xFF000000;
    @Setting(name = "Fade Speed", min = 0, max = 1, step = 0.05)
    public float fade = 0.25f;

    private final float[] anim = new float[7];
    private final Deque<Long> left = new ArrayDeque<Long>();
    private final Deque<Long> right = new ArrayDeque<Long>();

    public KeystrokesMod() {
        super("keystrokes", "Keystrokes", "WASD, mouse buttons and space light up on screen as you press them.", Category.HUD, false, 0f, 0.7f);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!event.buttonstate) return;
        if (event.button == 0) left.addLast(System.currentTimeMillis());
        if (event.button == 1) right.addLast(System.currentTimeMillis());
    }

    private static int cps(Deque<Long> q) {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
        return q.size();
    }

    private static boolean down(KeyBinding k) {
        int code = k.getKeyCode();
        if (code == 0) return false;
        return code < 0 ? Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code);
    }

    private static String keyName(KeyBinding k) {
        int code = k.getKeyCode();
        if (code < 0) return "M" + (code + 101);
        String n = Keyboard.getKeyName(code);
        return n == null ? "?" : n.length() > 4 ? n.substring(0, 3) : n;
    }

    @Override
    public int width() {
        return K * 3 + G * 2;
    }

    @Override
    public int height() {
        int h = K * 2 + G;
        if (mouse) h += K + G;
        if (space) h += 12 + G;
        return h;
    }

    private void key(int i, int x, int y, int w, int h, boolean pressed, String label, String sub) {
        anim[i] += ((pressed ? 1f : 0f) - anim[i]) * (fade <= 0 ? 1f : Math.min(1f, fade * 2));
        int bg = blend(idleColor, pressedColor, anim[i]);
        int fg = blend(textColor, pressedTextColor, anim[i]);
        Gui.drawRect(x, y, x + w, y + h, bg);
        if (sub == null) {
            mc.fontRendererObj.drawString(label, x + (w - mc.fontRendererObj.getStringWidth(label)) / 2, y + (h - 8) / 2, fg, false);
        } else {
            mc.fontRendererObj.drawString(label, x + (w - mc.fontRendererObj.getStringWidth(label)) / 2, y + 3, fg, false);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.translate(x + w / 2f, y + 13, 0);
            net.minecraft.client.renderer.GlStateManager.scale(0.6f, 0.6f, 1);
            mc.fontRendererObj.drawString(sub, -mc.fontRendererObj.getStringWidth(sub) / 2, 0, fg, false);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }

    private static int blend(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        net.minecraft.client.settings.GameSettings gs = mc.gameSettings;
        key(0, K + G, 0, K, K, down(gs.keyBindForward), keyName(gs.keyBindForward), null);
        key(1, 0, K + G, K, K, down(gs.keyBindLeft), keyName(gs.keyBindLeft), null);
        key(2, K + G, K + G, K, K, down(gs.keyBindBack), keyName(gs.keyBindBack), null);
        key(3, (K + G) * 2, K + G, K, K, down(gs.keyBindRight), keyName(gs.keyBindRight), null);
        int y = (K + G) * 2;
        if (mouse) {
            int w = (width() - G) / 2;
            key(4, 0, y, w, K, down(gs.keyBindAttack), "LMB", cps ? cps(left) + " CPS" : null);
            key(5, w + G, y, width() - w - G, K, down(gs.keyBindUseItem), "RMB", cps ? cps(right) + " CPS" : null);
            y += K + G;
        }
        if (space) {
            anim[6] += ((down(gs.keyBindJump) ? 1f : 0f) - anim[6]) * Math.min(1f, fade * 2);
            Gui.drawRect(0, y, width(), y + 12, blend(idleColor, pressedColor, anim[6]));
            int fg = blend(textColor, pressedTextColor, anim[6]);
            Gui.drawRect(width() / 2 - 12, y + 5, width() / 2 + 12, y + 7, fg);
        }
    }
}
