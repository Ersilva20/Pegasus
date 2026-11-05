package dev.mylauncher.core.util;

import dev.mylauncher.core.MLCore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Lets mods observe incoming packets (read-only), delivered on the client thread. */
public final class Packets {
    public interface Listener {
        void onPacket(Packet<?> packet);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<Listener>();

    public static void listen(Listener l) {
        if (!LISTENERS.contains(l)) LISTENERS.add(l);
    }

    public static void unlisten(Listener l) {
        LISTENERS.remove(l);
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            ChannelPipeline p = event.manager.channel().pipeline();
            if (p.get("ml_packets") != null || p.get("packet_handler") == null) return;
            p.addBefore("packet_handler", "ml_packets", new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof Packet && !LISTENERS.isEmpty()) {
                        final Packet<?> packet = (Packet<?>) msg;
                        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                            @Override
                            public void run() {
                                for (Listener l : LISTENERS) {
                                    try {
                                        l.onPacket(packet);
                                    } catch (Exception e) {
                                        MLCore.LOG.error("Packet listener failed", e);
                                    }
                                }
                            }
                        });
                    }
                    super.channelRead(ctx, msg);
                }
            });
        } catch (Exception e) {
            MLCore.LOG.error("Could not attach packet listener", e);
        }
    }
}
