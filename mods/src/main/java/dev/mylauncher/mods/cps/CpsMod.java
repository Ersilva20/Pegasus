package dev.mylauncher.mods.cps;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod(modid = "ml_cps", name = "CPS", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class CpsMod extends SimpleTextHud {
    @Setting(name = "Show Right Click")
    public boolean rightClick = true;

    private final Deque<Long> left = new ArrayDeque<Long>();
    private final Deque<Long> right = new ArrayDeque<Long>();

    public CpsMod() {
        super("cps", "CPS", "Shows how many times per second you click.", 0f, 0.06f);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!event.buttonstate) return;
        if (event.button == 0) left.addLast(System.currentTimeMillis());
        else if (event.button == 1) right.addLast(System.currentTimeMillis());
    }

    private static int count(Deque<Long> q) {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
        return q.size();
    }

    public int leftCps() {
        return count(left);
    }

    public int rightCps() {
        return count(right);
    }

    @Override
    protected String text() {
        return rightClick ? leftCps() + " | " + rightCps() + " CPS" : leftCps() + " CPS";
    }

    @Override
    protected String sample() {
        return rightClick ? "12 | 4 CPS" : "12 CPS";
    }
}
