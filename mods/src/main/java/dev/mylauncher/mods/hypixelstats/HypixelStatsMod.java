package dev.mylauncher.mods.hypixelstats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * /stats &lt;player&gt; [bedwars|duels|skywars] using the official Hypixel API. You need your own API key
 * (from the Hypixel Developer Dashboard); paste it in this mod's settings. Requests run off the game thread.
 */
@Mod(modid = "ml_hypixelstats", name = "Stats Lookup", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HypixelStatsMod extends Feature {
    @Setting(name = "Hypixel API Key")
    public String apiKey = "";

    private boolean registered;

    public HypixelStatsMod() {
        super("hypixelstats", "Stats Lookup", "Look up a player's Hypixel stats with /stats.", Category.HYPIXEL);
    }

    @Override
    protected void onEnable() {
        if (registered) return;
        registered = true;
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return "stats";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/stats <player> [bedwars|duels|skywars]";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                if (!isEnabled()) {
                    MLCore.chat("Stats Lookup is disabled.");
                    return;
                }
                if (apiKey.trim().isEmpty()) {
                    MLCore.chat("Set your Hypixel API key in the Stats Lookup settings first.");
                    return;
                }
                final String name = args.length > 0 ? args[0] : mc.thePlayer.getName();
                final String game = args.length > 1 ? args[1].toLowerCase() : "bedwars";
                final String knownUuid = uuidFromTab(name);
                Thread t = new Thread(() -> lookup(name, game, knownUuid), "Pegasus-HypixelStats");
                t.setDaemon(true);
                t.start();
            }

            @Override
            public int getRequiredPermissionLevel() {
                return 0;
            }

            @Override
            public boolean canCommandSenderUseCommand(ICommandSender sender) {
                return true;
            }
        });
    }

    private String uuidFromTab(String name) {
        if (mc.getNetHandler() == null) return null;
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (name.equalsIgnoreCase(info.getGameProfile().getName())) return info.getGameProfile().getId().toString().replace("-", "");
        }
        return null;
    }

    private static JsonObject get(String url, String key) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "Pegasus/1.0");
        if (key != null) c.setRequestProperty("API-Key", key);
        if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
        try (Reader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(r).getAsJsonObject();
        }
    }

    private static double num(JsonObject o, String key) {
        if (o == null) return 0;
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? 0 : e.getAsDouble();
    }

    private static String ratio(double a, double b) {
        return String.format("%.2f", b == 0 ? a : a / b);
    }

    private void reply(final String msg) {
        mc.addScheduledTask(() -> MLCore.chat(msg));
    }

    private void lookup(String name, String game, String uuid) {
        try {
            if (uuid == null) {
                JsonObject profile = get("https://api.mojang.com/users/profiles/minecraft/" + name, null);
                uuid = profile.get("id").getAsString();
            }
            JsonObject res = get("https://api.hypixel.net/v2/player?uuid=" + uuid, apiKey.trim());
            if (!res.has("player") || res.get("player").isJsonNull()) {
                reply(name + " has never joined Hypixel.");
                return;
            }
            JsonObject p = res.getAsJsonObject("player");
            String display = p.has("displayname") ? p.get("displayname").getAsString() : name;
            JsonObject stats = p.has("stats") ? p.getAsJsonObject("stats") : new JsonObject();
            String Y = EnumChatFormatting.YELLOW.toString(), W = EnumChatFormatting.WHITE.toString();
            if (game.startsWith("duel")) {
                JsonObject d = stats.has("Duels") ? stats.getAsJsonObject("Duels") : null;
                double w = num(d, "wins"), l = num(d, "losses"), k = num(d, "kills"), de = num(d, "deaths");
                reply(Y + display + W + " Duels - Wins " + (long) w + ", WLR " + ratio(w, l) + ", KDR " + ratio(k, de));
            } else if (game.startsWith("sky")) {
                JsonObject s = stats.has("SkyWars") ? stats.getAsJsonObject("SkyWars") : null;
                double w = num(s, "wins"), l = num(s, "losses"), k = num(s, "kills"), de = num(s, "deaths");
                reply(Y + display + W + " SkyWars - Wins " + (long) w + ", WLR " + ratio(w, l) + ", KDR " + ratio(k, de));
            } else {
                JsonObject b = stats.has("Bedwars") ? stats.getAsJsonObject("Bedwars") : null;
                JsonObject ach = p.has("achievements") ? p.getAsJsonObject("achievements") : null;
                double star = num(ach, "bedwars_level");
                double fk = num(b, "final_kills_bedwars"), fd = num(b, "final_deaths_bedwars");
                double w = num(b, "wins_bedwars"), l = num(b, "losses_bedwars");
                double bb = num(b, "beds_broken_bedwars");
                reply(Y + display + W + " Bed Wars - " + (long) star + "✫, FKDR " + ratio(fk, fd) + ", WLR " + ratio(w, l)
                        + ", Finals " + (long) fk + ", Beds " + (long) bb);
            }
        } catch (Exception e) {
            reply("Could not get stats for " + name + ": " + e.getMessage());
        }
    }
}
