package dev.mylauncher.mods.tps;

import dev.mylauncher.core.hud.SimpleTextHud;
import dev.mylauncher.core.util.Packets;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Servers send a time update every 20 server ticks. Measuring the real time between those packets
 * estimates the server's ticks per second.
 */
@Mod(modid = "ml_tps", name = "Server TPS", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class TpsMod extends SimpleTextHud implements Packets.Listener {
    private final long[] intervals = new long[10];
    private int count;
    private int index;
    private long lastPacket = -1;

    public TpsMod() {
        super("tps", "Server TPS", "Estimates how fast the server is running (20 TPS means no lag).", 1f, 0.24f);
    }

    @Override
    protected void onEnable() {
        Packets.listen(this);
    }

    @Override
    protected void onDisable() {
        Packets.unlisten(this);
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        count = 0;
        lastPacket = -1;
    }

    @Override
    public void onPacket(Packet<?> packet) {
        if (!(packet instanceof S03PacketTimeUpdate)) return;
        long now = System.currentTimeMillis();
        if (lastPacket > 0) {
            intervals[index] = now - lastPacket;
            index = (index + 1) % intervals.length;
            count = Math.min(count + 1, intervals.length);
        }
        lastPacket = now;
    }

    @Override
    protected String text() {
        if (mc.isSingleplayer()) return null;
        if (count == 0) return "TPS: ...";
        long sum = 0;
        for (int i = 0; i < count; i++) sum += intervals[i];
        double avg = sum / (double) count;
        double tps = Math.min(20.0, 20.0 * 1000.0 / Math.max(1, avg));
        String c = tps >= 19 ? "§a" : tps >= 15 ? "§e" : "§c";
        return "TPS: " + c + String.format("%.1f", tps);
    }

    @Override
    protected String sample() {
        return "TPS: 20.0";
    }
}
