package dev.mylauncher.mods.knockbacktrainer;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Jump-reset trainer: measures how many ticks your jump press was from the tick you took knockback.
 * 0 = perfect, negative = early, positive = late. Also graphs height above ground and keeps stats.
 */
@Mod(modid = "ml_knockbacktrainer", name = "Knockback Trainer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class KnockbackTrainerMod extends HudFeature {
    private static final int GRAPH = 60;

    @Setting(name = "Jump Timing Feedback")
    public boolean feedback = true;
    @Setting(name = "Ground Distance Graph")
    public boolean graph = true;
    @Setting(name = "Session Stats")
    public boolean stats = true;
    @Setting(name = "Window (ticks)", min = 2, max = 10, step = 1)
    public int window = 5;

    private long tick;
    private long lastHitTick = -1000;
    private long lastJumpTick = -1000;
    private boolean judged = true;
    private int lastHurt;
    private boolean wasJumpDown;
    private String message = "";
    private int messageColor = 0xFFFFFFFF;
    private long messageUntil;
    private int total, perfect, early, late;
    private long offsetSum;
    private final float[] heights = new float[GRAPH];
    private int heightIndex;
    private double groundY;

    public KnockbackTrainerMod() {
        super("knockbacktrainer", "Knockback Trainer", "Train jump resets: how close your jump press lands to the tick you were hit.",
                Category.PVP, false, 0.5f, 0.62f);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !inGame()) return;
        tick++;
        int hurt = mc.thePlayer.hurtTime;
        if (hurt > lastHurt && hurt == mc.thePlayer.maxHurtTime) {
            lastHitTick = tick;
            judged = false;
            total++;
        }
        lastHurt = hurt;

        boolean jumpDown = mc.gameSettings.keyBindJump.isKeyDown();
        if (jumpDown && !wasJumpDown) lastJumpTick = tick;
        wasJumpDown = jumpDown;

        if (!judged) {
            long since = tick - lastHitTick;
            long diff = lastJumpTick - lastHitTick;
            if (Math.abs(diff) <= window && lastJumpTick >= lastHitTick - window) {
                if (since >= Math.max(0, diff) || diff < 0) judge((int) diff);
            } else if (since > window) {
                judge(Integer.MAX_VALUE);
            }
        }

        if (mc.thePlayer.onGround) groundY = mc.thePlayer.posY;
        heights[heightIndex] = (float) Math.max(0, mc.thePlayer.posY - groundY);
        heightIndex = (heightIndex + 1) % GRAPH;
    }

    private void judge(int diff) {
        judged = true;
        if (diff == Integer.MAX_VALUE) {
            message = "No jump";
            messageColor = 0xFFAAAAAA;
        } else if (diff == 0) {
            message = "Perfect!";
            messageColor = 0xFF55FF55;
            perfect++;
            offsetSum += 0;
        } else if (diff < 0) {
            message = "Early by " + (-diff) + (diff == -1 ? " tick" : " ticks");
            messageColor = 0xFFFFFF55;
            early++;
            offsetSum += -diff;
        } else {
            message = "Late by " + diff + (diff == 1 ? " tick" : " ticks");
            messageColor = 0xFFFF5555;
            late++;
            offsetSum += diff;
        }
        messageUntil = System.currentTimeMillis() + 1500;
    }

    @Override
    public int width() {
        return 120;
    }

    @Override
    public int height() {
        int h = 0;
        if (feedback) h += 12;
        if (graph) h += 32;
        if (stats) h += 22;
        return Math.max(12, h);
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        int y = 0;
        if (feedback) {
            boolean show = System.currentTimeMillis() < messageUntil || editing;
            if (show) {
                String m = editing && message.isEmpty() ? "Perfect!" : message;
                mc.fontRendererObj.drawString(m, (120 - mc.fontRendererObj.getStringWidth(m)) / 2, y + 2, editing && message.isEmpty() ? 0xFF55FF55 : messageColor, true);
            }
            y += 12;
        }
        if (graph) {
            Gui.drawRect(0, y, 120, y + 30, 0x6F000000);
            float max = 1.3f;
            for (int i = 0; i < GRAPH; i++) {
                float h = heights[(heightIndex + i) % GRAPH];
                int bh = Math.round(Math.min(1f, h / max) * 28);
                Gui.drawRect(i * 2, y + 29 - bh, i * 2 + 2, y + 29, 0xFF5B8CFF);
            }
            y += 32;
        }
        if (stats) {
            int acc = total == 0 ? 0 : perfect * 100 / total;
            mc.fontRendererObj.drawString("Hits " + total + "  Perfect " + perfect + " (" + acc + "%)", 0, y + 1, 0xFFFFFFFF, true);
            int judgedCount = perfect + early + late;
            String avg = judgedCount == 0 ? "-" : String.format("%.1f", offsetSum / (double) judgedCount);
            mc.fontRendererObj.drawString("Early " + early + "  Late " + late + "  Avg " + avg, 0, y + 11, 0xFFAAAAAA, true);
        }
    }
}
