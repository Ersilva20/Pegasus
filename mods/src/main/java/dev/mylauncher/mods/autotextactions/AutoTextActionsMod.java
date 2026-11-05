package dev.mylauncher.mods.autotextactions;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * When a received chat message contains a trigger, send a reply. Each rule is "trigger => reply".
 * Automatic replies can be against some servers' rules; check before using.
 */
@Mod(modid = "ml_autotextactions", name = "Auto Text Actions", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class AutoTextActionsMod extends Feature {
    @Setting(name = "Rule 1 (trigger => reply)")
    public String rule1 = "";
    @Setting(name = "Rule 2 (trigger => reply)")
    public String rule2 = "";
    @Setting(name = "Rule 3 (trigger => reply)")
    public String rule3 = "";
    @Setting(name = "Rule 4 (trigger => reply)")
    public String rule4 = "";
    @Setting(name = "Rule 5 (trigger => reply)")
    public String rule5 = "";
    @Setting(name = "Case Sensitive")
    public boolean caseSensitive = false;
    @Setting(name = "Delay (ms)", min = 0, max = 5000, step = 100)
    public int delay = 500;
    @Setting(name = "Cooldown Per Rule (seconds)", min = 1, max = 120, step = 1)
    public int cooldown = 10;

    private final long[] lastFired = new long[5];
    private String pending;
    private long pendingAt;

    public AutoTextActionsMod() {
        super("autotextactions", "Auto Text Actions", "Set up actions that are triggered on received chat messages.", Category.CHAT);
    }

    private String rule(int i) {
        switch (i) {
            case 0: return rule1;
            case 1: return rule2;
            case 2: return rule3;
            case 3: return rule4;
            default: return rule5;
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2 || event.message == null || pending != null) return;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        // Never react to our own messages.
        if (text.contains(mc.getSession().getUsername() + ":")) return;
        for (int i = 0; i < 5; i++) {
            String r = rule(i);
            int sep = r.indexOf("=>");
            if (sep <= 0) continue;
            String trigger = r.substring(0, sep).trim(), reply = r.substring(sep + 2).trim();
            if (trigger.isEmpty() || reply.isEmpty()) continue;
            boolean hit = caseSensitive ? text.contains(trigger) : text.toLowerCase().contains(trigger.toLowerCase());
            if (!hit || System.currentTimeMillis() - lastFired[i] < cooldown * 1000L) continue;
            lastFired[i] = System.currentTimeMillis();
            pending = reply;
            pendingAt = System.currentTimeMillis() + delay;
            return;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending == null || System.currentTimeMillis() < pendingAt) return;
        if (mc.thePlayer != null) mc.thePlayer.sendChatMessage(pending);
        pending = null;
    }
}
