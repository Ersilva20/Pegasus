package dev.mylauncher.core.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mylauncher.core.Category;
import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Pegasus cosmetics (capes), built into the core: always on, opened from the COSMETICS tab of the menu,
 * the O key or /wardrobe. You always see your own cape. Other Pegasus players' capes are shown when a shared
 * registry URL is set (a JSON file such as a GitHub Gist: {"uuid": {"cape": "..."}}).
 */
public class CosmeticsMod extends Feature implements Hooks.SkinOverride {
    @Setting(name = "Equipped Cape")
    public String cape = "pegasus";
    @Setting(name = "Show Other Players' Capes")
    public boolean showOthers = true;
    @Setting(name = "Shared Registry URL (optional)", description = "JSON file listing other Pegasus players' capes")
    public String registryUrl = "";

    final CapeTextures capes = new CapeTextures();
    private final KeyBinding key = MLCore.key("Open Wardrobe", Keyboard.KEY_O);
    private volatile Map<String, String> others = Collections.emptyMap();
    private boolean commandAdded;
    private long nextFetch;
    private int ticks;

    public CosmeticsMod() {
        super("cosmetics", "Cosmetics", "Capes. Press O to open the Wardrobe.", Category.VISUAL, true);
    }

    @Override
    public boolean builtIn() {
        return true;
    }

    public void equip(String id) {
        cape = id;
        Config.save(this);
    }

    private boolean isMe(AbstractClientPlayer p) {
        return p == mc.thePlayer || p.getUniqueID().equals(mc.getSession().getProfile().getId())
                || p.getName().equals(mc.getSession().getUsername());
    }

    /** Cape id for a player, or null for none. */
    public String capeFor(AbstractClientPlayer p) {
        if (isMe(p)) return cape;
        if (!showOthers) return null;
        return others.get(p.getUniqueID().toString().replace("-", "").toLowerCase());
    }

    /** Cape hook: Minecraft's own cape layer (or Cape Physics) draws our texture. */
    @Override
    public ResourceLocation skin(AbstractClientPlayer player) {
        String id = capeFor(player);
        return id == null ? null : capes.get(id);
    }

    @Override
    public void onSettingsChanged() {
        nextFetch = 0;
    }

    @Override
    protected void onEnable() {
        Hooks.cape = this;
        if (!commandAdded) {
            commandAdded = true;
            ClientCommandHandler.instance.registerCommand(new CommandBase() {
                @Override
                public String getCommandName() {
                    return "wardrobe";
                }

                @Override
                public String getCommandUsage(ICommandSender sender) {
                    return "/wardrobe - open the Pegasus cape wardrobe";
                }

                @Override
                public void processCommand(ICommandSender sender, String[] args) {
                    MLCore.openNextTick(new WardrobeScreen(null, CosmeticsMod.this));
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
    }

    @Override
    protected void onDisable() {
        if (Hooks.cape == this) Hooks.cape = null;
    }

    @Override
    public GuiScreen customScreen(GuiScreen parent) {
        return new WardrobeScreen(parent, this);
    }

    @SubscribeEvent
    public void onKey(InputEvent event) {
        if (key.isPressed() && mc.currentScreen == null && mc.thePlayer != null) mc.displayGuiScreen(new WardrobeScreen(null, this));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks % 2 == 0) capes.tick(ticks);
        if (showOthers && !registryUrl.trim().isEmpty() && System.currentTimeMillis() > nextFetch) {
            nextFetch = System.currentTimeMillis() + 5 * 60 * 1000L;
            final String url = registryUrl.trim();
            Thread t = new Thread(() -> fetchRegistry(url), "Pegasus-Cosmetics");
            t.setDaemon(true);
            t.start();
        }
    }

    private void fetchRegistry(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "Pegasus/1.0");
            try (Reader r = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(r).getAsJsonObject();
                Map<String, String> map = new HashMap<String, String>();
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    if (!e.getValue().isJsonObject()) continue;
                    JsonObject o = e.getValue().getAsJsonObject();
                    if (o.has("cape")) map.put(e.getKey().replace("-", "").toLowerCase(), safe(o.get("cape").getAsString()));
                }
                others = map;
            }
        } catch (Exception e) {
            MLCore.LOG.warn("Could not load cosmetics registry: {}", e.toString());
        }
    }

    /** Registry entries may only use built-in capes (no local file capes or unknown ids from other people). */
    private static String safe(String id) {
        for (Catalog.Item item : Catalog.CAPES) if (item.id.equals(id)) return id;
        return Catalog.NONE;
    }
}
