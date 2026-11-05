package dev.mylauncher.mods.worldeditcui;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.nio.charset.StandardCharsets;

/** Shows your WorldEdit cuboid selection using the WECUI plugin channel. */
@Mod(modid = "ml_worldeditcui", name = "WorldEdit CUI", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class WorldEditCuiMod extends Feature {
    private static final String CHANNEL = "WECUI";

    @Setting(name = "Selection Color", color = true)
    public int color = 0xFFFF3333;
    @Setting(name = "Grid Color", color = true)
    public int gridColor = 0x80FFFFFF;
    @Setting(name = "Show Grid")
    public boolean grid = true;

    private FMLEventChannel channel;
    private int[] p1, p2;

    public WorldEditCuiMod() {
        super("worldeditcui", "WorldEdit CUI", "Shows your WorldEdit selection in the world.", Category.WORLD);
    }

    @Override
    protected void onEnable() {
        if (channel == null) {
            channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
            channel.register(this);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.entity == mc.thePlayer && channel != null && isEnabled()) {
            p1 = p2 = null;
            ByteBuf buf = Unpooled.copiedBuffer("v|3", StandardCharsets.UTF_8);
            try {
                channel.sendToServer(new FMLProxyPacket(new PacketBuffer(buf), CHANNEL));
            } catch (Exception ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        if (!isEnabled()) return;
        String msg = event.packet.payload().toString(StandardCharsets.UTF_8);
        String[] parts = msg.split("\\|");
        if (parts.length == 0) return;
        if (parts[0].equals("s")) {
            p1 = p2 = null;
        } else if (parts[0].equals("p") && parts.length >= 5) {
            try {
                int[] p = {Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])};
                if (parts[1].equals("0")) p1 = p;
                else if (parts[1].equals("1")) p2 = p;
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (p1 == null && p2 == null) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        RenderUtil.beginWorldLines(2.5f);
        if (p1 != null) RenderUtil.boxOutline(box(p1, p1).offset(-vx, -vy, -vz), 0xFF33FF33);
        if (p2 != null) RenderUtil.boxOutline(box(p2, p2).offset(-vx, -vy, -vz), 0xFF3333FF);
        if (p1 != null && p2 != null) {
            AxisAlignedBB bb = box(p1, p2).offset(-vx, -vy, -vz);
            RenderUtil.boxOutline(bb, color);
            if (grid) {
                net.minecraft.client.renderer.GlStateManager.enableDepth();
                for (double x = bb.minX + 1; x < bb.maxX; x++) {
                    RenderUtil.line(x, bb.minY, bb.minZ, x, bb.maxY, bb.minZ, gridColor);
                    RenderUtil.line(x, bb.minY, bb.maxZ, x, bb.maxY, bb.maxZ, gridColor);
                }
                for (double z = bb.minZ + 1; z < bb.maxZ; z++) {
                    RenderUtil.line(bb.minX, bb.minY, z, bb.minX, bb.maxY, z, gridColor);
                    RenderUtil.line(bb.maxX, bb.minY, z, bb.maxX, bb.maxY, z, gridColor);
                }
            }
        }
        RenderUtil.endWorldLines();
    }

    private static AxisAlignedBB box(int[] a, int[] b) {
        return new AxisAlignedBB(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[0], b[0]) + 1, Math.max(a[1], b[1]) + 1, Math.max(a[2], b[2]) + 1);
    }
}
