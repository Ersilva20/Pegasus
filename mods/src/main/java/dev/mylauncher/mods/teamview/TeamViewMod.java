package dev.mylauncher.mods.teamview;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.TextHud;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Teammates = players on the same scoreboard team as you (how most minigames mark teams).
 * Shows a marker above them (through walls) and a HUD list with distance and health.
 */
@Mod(modid = "ml_teamview", name = "Team View", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class TeamViewMod extends TextHud {
    @Setting(name = "World Markers")
    public boolean markers = true;
    @Setting(name = "Marker Color", color = true)
    public int markerColor = 0xFF55FFFF;
    @Setting(name = "Use Team Color")
    public boolean teamColor = true;
    @Setting(name = "HUD List")
    public boolean list = true;

    public TeamViewMod() {
        super("teamview", "Team View", "Shows you where your teammates are on the HUD.", Category.PVP, false, 0f, 0.8f);
        fixedWidth = false;
    }

    private List<EntityPlayer> teammates() {
        List<EntityPlayer> out = new ArrayList<EntityPlayer>();
        ScorePlayerTeam mine = mc.theWorld.getScoreboard().getPlayersTeam(mc.thePlayer.getName());
        if (mine == null) return out;
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer) continue;
            ScorePlayerTeam t = mc.theWorld.getScoreboard().getPlayersTeam(p.getName());
            if (t != null && t.getRegisteredName().equals(mine.getRegisteredName())) out.add(p);
        }
        return out;
    }

    private int colorOf(EntityPlayer p) {
        if (!teamColor) return markerColor;
        ScorePlayerTeam t = mc.theWorld.getScoreboard().getPlayersTeam(p.getName());
        if (t == null) return markerColor;
        EnumChatFormatting f = t.getChatFormat();
        int idx = f == null ? -1 : f.getColorIndex();
        if (idx < 0) return markerColor;
        int c = mc.fontRendererObj.getColorCode(f.toString().charAt(1));
        return 0xFF000000 | c;
    }

    @Override
    public boolean shouldRender() {
        return list && super.shouldRender();
    }

    @Override
    protected List<String> lines() {
        List<String> out = new ArrayList<String>();
        for (EntityPlayer p : teammates()) {
            out.add(p.getName() + " §7" + Math.round(p.getDistanceToEntity(mc.thePlayer)) + "m §c" + Math.round(p.getHealth()) + "❤");
        }
        return out;
    }

    @Override
    protected List<String> sampleLines() {
        return Arrays.asList("Teammate §712m §c20❤");
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!markers || !inGame()) return;
        double cx = mc.getRenderManager().viewerPosX, cy = mc.getRenderManager().viewerPosY, cz = mc.getRenderManager().viewerPosZ;
        for (EntityPlayer p : teammates()) {
            double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * event.partialTicks - cx;
            double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * event.partialTicks - cy + p.height + 0.9;
            double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * event.partialTicks - cz;
            double dist = Math.sqrt(x * x + y * y + z * z);
            float scale = (float) Math.max(1.0, dist / 8.0);
            RenderUtil.worldLabel("▼", x, y, z, colorOf(p), scale * 1.5f, false);
        }
    }
}
