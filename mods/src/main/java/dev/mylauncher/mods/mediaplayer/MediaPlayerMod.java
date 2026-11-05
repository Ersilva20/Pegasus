package dev.mylauncher.mods.mediaplayer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Shows what Spotify is playing (read from the Spotify window title) and sends media keys
 * (play/pause, next, previous) to control any media app. Windows only.
 */
@Mod(modid = "ml_mediaplayer", name = "Media Player", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MediaPlayerMod extends SimpleTextHud {
    interface User32 extends Library {
        void keybd_event(byte vk, byte scan, int flags, Pointer extra);
    }

    @Setting(name = "Show When Paused")
    public boolean showPaused = false;

    private final KeyBinding playPause = MLCore.key("Media: Play / Pause", Keyboard.KEY_NONE);
    private final KeyBinding next = MLCore.key("Media: Next Track", Keyboard.KEY_NONE);
    private final KeyBinding previous = MLCore.key("Media: Previous Track", Keyboard.KEY_NONE);
    private final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");

    private volatile String track;
    private Thread poller;
    private User32 user32;

    public MediaPlayerMod() {
        super("mediaplayer", "Media Player", "See and control the media playing on your computer, like Spotify.", 0.5f, 0.92f);
    }

    @Override
    protected void onEnable() {
        if (!windows || poller != null) return;
        poller = new Thread(() -> {
            while (poller != null) {
                track = readSpotifyTitle();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "Pegasus-MediaPlayer");
        poller.setDaemon(true);
        poller.start();
    }

    @Override
    protected void onDisable() {
        Thread t = poller;
        poller = null;
        if (t != null) t.interrupt();
    }

    /** "Artist - Song" while playing; Spotify shows just "Spotify ..." when paused. */
    private static String readSpotifyTitle() {
        try {
            Process p = new ProcessBuilder("tasklist", "/v", "/fo", "csv", "/nh", "/fi", "imagename eq Spotify.exe").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] cols = line.split("\",\"");
                    if (cols.length < 9) continue;
                    String title = cols[cols.length - 1].replace("\"", "").trim();
                    if (title.isEmpty() || title.equals("N/A") || title.startsWith("Spotify") || title.startsWith("GDI+")
                            || title.equals("Default IME") || title.equals("MSCTFIME UI")) continue;
                    return title;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void press(int vk) {
        if (!windows) return;
        try {
            if (user32 == null) user32 = (User32) Native.loadLibrary("user32", User32.class);
            user32.keybd_event((byte) vk, (byte) 0, 0, null);
            user32.keybd_event((byte) vk, (byte) 0, 2, null);
        } catch (Throwable t) {
            MLCore.LOG.warn("Media keys unavailable", t);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (playPause.isPressed()) press(0xB3);
        if (next.isPressed()) press(0xB0);
        if (previous.isPressed()) press(0xB1);
    }

    @Override
    protected String text() {
        String t = track;
        if (t == null) return showPaused ? "♫ Paused" : null;
        return "♫ " + t;
    }

    @Override
    protected String sample() {
        return "♫ Artist - Song";
    }
}
