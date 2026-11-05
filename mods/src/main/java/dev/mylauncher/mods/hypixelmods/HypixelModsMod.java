package dev.mylauncher.mods.hypixelmods;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Pattern;

/** Auto GG, Auto Tip and lobby message hiding for Hypixel. Only active while connected to Hypixel. */
@Mod(modid = "ml_hypixelmods", name = "Hypixel Toolkit", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HypixelModsMod extends Feature {
    private static final String[] GAME_END = {
            "1st Killer - ", "1st Place - ", "Winner: ", " - Damage Dealt - ", "Winning Team - ", "1st - ",
            "Winners: ", "Winner - ", "Top Survivors", "Your Overall Winstreak:", "Winning Team:"
    };
    private static final Pattern LOBBY_JOIN = Pattern.compile(".*(joined|spooked into|slid into|entered|sled into|fell into|was summoned into) the lobby!.*");

    @Setting(name = "Auto GG")
    public boolean autoGg = true;
    @Setting(name = "GG Message")
    public String ggMessage = "gg";
    @Setting(name = "GG Delay (ms)", min = 0, max = 5000, step = 100)
    public int ggDelay = 1000;
    @Setting(name = "Auto Tip (every 15 min)")
    public boolean autoTip = false;
    @Setting(name = "Hide Lobby Join Messages")
    public boolean hideLobby = true;
    @Setting(name = "Hide Mystery Box / Ticket Spam")
    public boolean hideBoxes = true;

    private long ggAt = -1;
    private long lastGg;
    private long lastTip;

    public HypixelModsMod() {
        super("hypixelmods", "Hypixel Toolkit", "Auto GG, auto tip and a lobby spam filter for Hypixel.", Category.HYPIXEL);
    }

    private boolean onHypixel() {
        return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.toLowerCase().contains("hypixel");
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!onHypixel() || event.message == null || event.type == 2) return;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (hideLobby && LOBBY_JOIN.matcher(text).matches()) {
            event.setCanceled(true);
            return;
        }
        if (hideBoxes && (text.contains("found a ") && text.contains("Mystery Box") || text.startsWith("[Mystery Box]"))) {
            event.setCanceled(true);
            return;
        }
        if (autoGg && System.currentTimeMillis() - lastGg > 10000) {
            String trimmed = text.trim();
            for (String t : GAME_END) {
                if (trimmed.startsWith(t.trim()) || text.contains(t)) {
                    ggAt = System.currentTimeMillis() + ggDelay;
                    lastGg = System.currentTimeMillis();
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || !onHypixel()) return;
        long now = System.currentTimeMillis();
        if (ggAt > 0 && now >= ggAt) {
            ggAt = -1;
            mc.thePlayer.sendChatMessage("/ac " + ggMessage);
        }
        if (autoTip && now - lastTip > 15 * 60 * 1000L) {
            lastTip = now;
            mc.thePlayer.sendChatMessage("/tip all");
        }
    }
}
