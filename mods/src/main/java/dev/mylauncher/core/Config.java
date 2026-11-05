package dev.mylauncher.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mylauncher.core.hud.HudFeature;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** Saves each feature to .minecraft/config/pegasus/&lt;id&gt;.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {
    }

    public static File dir() {
        File d = new File(Minecraft.getMinecraft().mcDataDir, "config/pegasus");
        // Settings from before the rename (config/mylauncher) are moved over once.
        File legacy = new File(Minecraft.getMinecraft().mcDataDir, "config/mylauncher");
        if (!d.exists() && legacy.isDirectory()) legacy.renameTo(d);
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    private static File file(Feature f) {
        return new File(dir(), f.id + ".json");
    }

    public static void load(Feature f) {
        File file = file(f);
        if (!file.isFile()) {
            f.setEnabled(f.enabledByDefault);
            return;
        }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject o = new JsonParser().parse(r).getAsJsonObject();
            if (o.has("settings")) {
                JsonObject s = o.getAsJsonObject("settings");
                for (SettingField sf : f.settings()) {
                    JsonElement e = s.get(sf.key());
                    if (e == null || e.isJsonNull()) continue;
                    try {
                        read(sf, e);
                    } catch (Exception ex) {
                        MLCore.LOG.warn("Bad value for {}.{}: {}", f.id, sf.key(), e);
                    }
                }
            }
            if (f instanceof HudFeature && o.has("hud")) {
                JsonObject h = o.getAsJsonObject("hud");
                HudFeature hf = (HudFeature) f;
                hf.relX = h.get("x").getAsFloat();
                hf.relY = h.get("y").getAsFloat();
            }
            f.setEnabled(o.has("enabled") ? o.get("enabled").getAsBoolean() : f.enabledByDefault);
            f.onSettingsChanged();
        } catch (Exception e) {
            MLCore.LOG.error("Could not load config for " + f.id, e);
            f.setEnabled(f.enabledByDefault);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void read(SettingField sf, JsonElement e) {
        switch (sf.type) {
            case BOOLEAN:
                sf.set(e.getAsBoolean());
                break;
            case INT:
            case COLOR:
                sf.set(e.getAsInt());
                break;
            case FLOAT:
                sf.set(e.getAsFloat());
                break;
            case DOUBLE:
                sf.set(e.getAsDouble());
                break;
            case STRING:
                sf.set(e.getAsString());
                break;
            case ENUM:
                sf.set(Enum.valueOf((Class<Enum>) sf.field.getType(), e.getAsString()));
                break;
        }
    }

    public static void save(Feature f) {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", f.isEnabled());
        JsonObject s = new JsonObject();
        for (SettingField sf : f.settings()) {
            Object v = sf.get();
            if (v instanceof Boolean) s.addProperty(sf.key(), (Boolean) v);
            else if (v instanceof Number) s.addProperty(sf.key(), (Number) v);
            else if (v instanceof Enum) s.addProperty(sf.key(), ((Enum<?>) v).name());
            else s.addProperty(sf.key(), String.valueOf(v));
        }
        o.add("settings", s);
        if (f instanceof HudFeature) {
            HudFeature hf = (HudFeature) f;
            JsonObject h = new JsonObject();
            h.addProperty("x", hf.relX);
            h.addProperty("y", hf.relY);
            o.add("hud", h);
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file(f)), StandardCharsets.UTF_8)) {
            GSON.toJson(o, w);
        } catch (Exception e) {
            MLCore.LOG.error("Could not save config for " + f.id, e);
        }
    }

    /** Shared JSON helpers for mods that store extra data (waypoints, auto text, etc.). */
    public static <T> T readJson(String name, Class<T> type, T fallback) {
        File file = new File(dir(), name);
        if (!file.isFile()) return fallback;
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            T t = GSON.fromJson(r, type);
            return t != null ? t : fallback;
        } catch (Exception e) {
            MLCore.LOG.error("Could not read " + name, e);
            return fallback;
        }
    }

    public static void writeJson(String name, Object value) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir(), name)), StandardCharsets.UTF_8)) {
            GSON.toJson(value, w);
        } catch (Exception e) {
            MLCore.LOG.error("Could not write " + name, e);
        }
    }
}
