package dev.mylauncher.mods.quickplay;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.gui.Theme;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/** Press J for a menu of Hypixel games; click one to join (/play ...). */
@Mod(modid = "ml_quickplay", name = "Hypixel Quickplay", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class QuickplayMod extends Feature {
    static final String[][][] GAMES = {
            {{"Bed Wars"}, {"Solo", "/play bedwars_eight_one"}, {"Doubles", "/play bedwars_eight_two"}, {"3v3v3v3", "/play bedwars_four_three"}, {"4v4v4v4", "/play bedwars_four_four"}, {"4v4", "/play bedwars_two_four"}, {"Lobby", "/l bedwars"}},
            {{"SkyWars"}, {"Solo Normal", "/play solo_normal"}, {"Solo Insane", "/play solo_insane"}, {"Teams Normal", "/play teams_normal"}, {"Teams Insane", "/play teams_insane"}, {"Lobby", "/l skywars"}},
            {{"Duels"}, {"Classic", "/play duels_classic_duel"}, {"Sumo", "/play duels_sumo_duel"}, {"Bridge", "/play duels_bridge_duel"}, {"UHC", "/play duels_uhc_duel"}, {"OP", "/play duels_op_duel"}, {"Bow", "/play duels_bow_duel"}, {"NoDebuff", "/play duels_potion_duel"}, {"Combo", "/play duels_combo_duel"}, {"Lobby", "/l duels"}},
            {{"Murder Mystery"}, {"Classic", "/play murder_classic"}, {"Double Up", "/play murder_double_up"}, {"Assassins", "/play murder_assassins"}, {"Lobby", "/l murder"}},
            {{"TNT Games"}, {"TNT Run", "/play tnt_tntrun"}, {"PvP Run", "/play tnt_pvprun"}, {"Bow Spleef", "/play tnt_bowspleef"}, {"Wizards", "/play tnt_capture"}, {"Lobby", "/l tnt"}},
            {{"Other"}, {"Build Battle", "/play build_battle_solo_normal"}, {"UHC Solo", "/play uhc_solo"}, {"Mega Walls", "/play mw_standard"}, {"Blitz SG", "/play blitz_solo_normal"}, {"Party Games", "/play arcade_party_games_1"}, {"Main Lobby", "/l main"}, {"SkyBlock", "/play skyblock"}}
    };

    private final KeyBinding key = MLCore.key("Open Quickplay", Keyboard.KEY_J);

    public QuickplayMod() {
        super("quickplay", "Hypixel Quickplay", "Quickly join games on the Hypixel network", Category.HYPIXEL);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (key.isPressed() && mc.currentScreen == null && mc.thePlayer != null) mc.displayGuiScreen(new Screen());
    }

    static final class Screen extends GuiScreen {
        private int category;
        private int scroll;

        @Override
        public void drawScreen(int mx, int my, float pt) {
            drawRect(0, 0, width, height, Theme.OVERLAY);
            int pw = 300, ph = 200, px = (width - pw) / 2, py = (height - ph) / 2;
            RenderUtil.softRect(px, py, px + pw, py + ph, Theme.PANEL);
            fontRendererObj.drawStringWithShadow("Hypixel Quickplay", px + 8, py + 8, Theme.TEXT);
            for (int i = 0; i < GAMES.length; i++) {
                int y = py + 24 + i * 18;
                boolean sel = i == category, hov = mx >= px + 6 && mx < px + 96 && my >= y && my < y + 16;
                if (sel || hov) RenderUtil.softRect(px + 6, y, px + 96, y + 16, sel ? Theme.CARD_HOVER : Theme.CARD);
                fontRendererObj.drawString(GAMES[i][0][0], px + 12, y + 4, sel ? Theme.TEXT : Theme.TEXT_DIM);
            }
            String[][] games = GAMES[category];
            for (int i = 1; i < games.length; i++) {
                int col = (i - 1) % 2, row = (i - 1) / 2;
                int x = px + 104 + col * 96, y = py + 24 + row * 24 - scroll;
                boolean hov = mx >= x && mx < x + 90 && my >= y && my < y + 20;
                RenderUtil.softRect(x, y, x + 90, y + 20, hov ? Theme.ACCENT : Theme.CARD);
                drawCenteredString(fontRendererObj, games[i][0], x + 45, y + 6, Theme.TEXT);
            }
            super.drawScreen(mx, my, pt);
        }

        @Override
        protected void mouseClicked(int mx, int my, int button) throws IOException {
            int pw = 300, ph = 200, px = (width - pw) / 2, py = (height - ph) / 2;
            for (int i = 0; i < GAMES.length; i++) {
                int y = py + 24 + i * 18;
                if (mx >= px + 6 && mx < px + 96 && my >= y && my < y + 16) {
                    category = i;
                    scroll = 0;
                    return;
                }
            }
            String[][] games = GAMES[category];
            for (int i = 1; i < games.length; i++) {
                int col = (i - 1) % 2, row = (i - 1) / 2;
                int x = px + 104 + col * 96, y = py + 24 + row * 24 - scroll;
                if (mx >= x && mx < x + 90 && my >= y && my < y + 20) {
                    mc.thePlayer.sendChatMessage(games[i][1]);
                    mc.displayGuiScreen(null);
                    return;
                }
            }
        }

        @Override
        public void handleMouseInput() throws IOException {
            super.handleMouseInput();
            int w = Mouse.getEventDWheel();
            if (w != 0) scroll = Math.max(0, scroll - Integer.signum(w) * 12);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
