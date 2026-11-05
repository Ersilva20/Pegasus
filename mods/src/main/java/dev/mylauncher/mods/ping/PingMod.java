package dev.mylauncher.mods.ping;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_ping", name = "Ping", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PingMod extends SimpleTextHud {
    @Setting(name = "Show on HUD")
    public boolean hud = true;

    @Setting(name = "Show on Nametags", description = "Shows each player's latency above their name")
    public boolean nametags = false;

    @Setting(name = "Colored Text", description = "Green / yellow / red by latency")
    public boolean colored = true;

    public PingMod() {
        super("ping", "Ping", "Shows your connection delay to the server, in milliseconds.", 0f, 0.18f);
    }

    private int ping(EntityPlayer player) {
        if (mc.getNetHandler() == null) return -1;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        return info == null ? -1 : info.getResponseTime();
    }

    private String format(int ms) {
        String c = !colored ? "" : ms < 80 ? "§a" : ms < 150 ? "§e" : ms < 250 ? "§6" : "§c";
        return c + ms + " ms";
    }

    @Override
    public boolean shouldRender() {
        return hud && super.shouldRender();
    }

    @Override
    protected String text() {
        if (mc.isSingleplayer()) return null;
        int p = ping(mc.thePlayer);
        return p < 0 ? null : format(p);
    }

    @Override
    protected String sample() {
        return format(42);
    }

    @SubscribeEvent
    public void onNametag(RenderLivingEvent.Specials.Post<?> event) {
        if (!nametags || !(event.entity instanceof EntityPlayer) || mc.isSingleplayer()) return;
        EntityPlayer p = (EntityPlayer) event.entity;
        if (p == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;
        if (p.isInvisible() || p.getDistanceSqToEntity(mc.thePlayer) > 64 * 64) return;
        int ms = ping(p);
        if (ms <= 0) return;
        RenderUtil.worldLabel(format(ms), event.x, event.y + p.height + 0.75, event.z, 0xFFFFFFFF, 0.8f, true);
    }
}
