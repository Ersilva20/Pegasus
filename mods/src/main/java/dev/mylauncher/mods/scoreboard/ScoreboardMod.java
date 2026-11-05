package dev.mylauncher.mods.scoreboard;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import dev.mylauncher.core.Category;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Movable, scalable sidebar scoreboard (drag it in the HUD editor). */
@Mod(modid = "ml_scoreboard", name = "Scoreboard", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ScoreboardMod extends HudFeature implements Hooks.ScoreboardRenderer {
    @Setting(name = "Hide Scoreboard")
    public boolean hide = false;
    @Setting(name = "Show Red Numbers")
    public boolean numbers = false;
    @Setting(name = "Text Shadow")
    public boolean shadow = false;
    @Setting(name = "Background Color", color = true)
    public int background = 0x50000000;
    @Setting(name = "Title Background Color", color = true)
    public int titleBackground = 0x60000000;

    public ScoreboardMod() {
        super("scoreboard", "Scoreboard", "Move, resize or hide the sidebar scoreboard.", Category.HUD, false, 1f, 0.5f);
    }

    @Override
    protected void onEnable() {
        Hooks.scoreboard = this;
    }

    @Override
    protected void onDisable() {
        if (Hooks.scoreboard == this) Hooks.scoreboard = null;
    }

    /** Vanilla is always suppressed while this mod is on; we draw it ourselves as a HUD element. */
    @Override
    public boolean render(ScoreObjective objective, ScaledResolution resolution) {
        return true;
    }

    private ScoreObjective objective() {
        if (!inGame()) return null;
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScorePlayerTeam team = sb.getPlayersTeam(mc.thePlayer.getName());
        if (team != null) {
            int slot = team.getChatFormat().getColorIndex();
            if (slot >= 0) {
                ScoreObjective o = sb.getObjectiveInDisplaySlot(3 + slot);
                if (o != null) return o;
            }
        }
        return sb.getObjectiveInDisplaySlot(1);
    }

    private List<Score> scores(ScoreObjective obj) {
        Scoreboard sb = obj.getScoreboard();
        Collection<Score> all = sb.getSortedScores(obj);
        List<Score> filtered = new ArrayList<Score>();
        for (Score s : all) if (s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) filtered.add(s);
        if (filtered.size() > 15) filtered = Lists.newArrayList(Iterables.skip(filtered, filtered.size() - 15));
        return filtered;
    }

    private String line(Score s) {
        ScorePlayerTeam t = s.getScoreScoreboard().getPlayersTeam(s.getPlayerName());
        return ScorePlayerTeam.formatPlayerName(t, s.getPlayerName());
    }

    @Override
    public boolean shouldRender() {
        return !hide && objective() != null;
    }

    @Override
    public int width() {
        ScoreObjective o = objective();
        if (o == null) return 80;
        int w = mc.fontRendererObj.getStringWidth(o.getDisplayName());
        for (Score s : scores(o)) {
            int lw = mc.fontRendererObj.getStringWidth(line(s));
            if (numbers) lw += 3 + mc.fontRendererObj.getStringWidth(String.valueOf(s.getScorePoints()));
            w = Math.max(w, lw);
        }
        return w + 4;
    }

    @Override
    public int height() {
        ScoreObjective o = objective();
        return o == null ? 60 : (scores(o).size() + 1) * 9 + 1;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        ScoreObjective o = objective();
        int w = width();
        if (o == null) {
            Gui.drawRect(0, 0, w, 10, titleBackground);
            mc.fontRendererObj.drawString("Scoreboard", 2, 1, 0xFFFFFFFF, shadow);
            Gui.drawRect(0, 10, w, 60, background);
            return;
        }
        List<Score> list = scores(o);
        String title = o.getDisplayName();
        Gui.drawRect(0, 0, w, 10, titleBackground);
        mc.fontRendererObj.drawString(title, (w - mc.fontRendererObj.getStringWidth(title)) / 2, 1, 0xFFFFFFFF, shadow);
        int y = 10;
        for (int i = list.size() - 1; i >= 0; i--) {
            Score s = list.get(i);
            Gui.drawRect(0, y, w, y + 9, background);
            mc.fontRendererObj.drawString(line(s), 2, y, 0xFFFFFFFF, shadow);
            if (numbers) {
                String n = "§c" + s.getScorePoints();
                mc.fontRendererObj.drawString(n, w - 2 - mc.fontRendererObj.getStringWidth(n), y, 0xFFFFFFFF, shadow);
            }
            y += 9;
        }
    }
}
