package dev.mylauncher.core.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import dev.mylauncher.core.MLCore;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Fixes LWJGL 2's side-button bug on Windows. On WM_XBUTTONDOWN it decides between mouse button 4 and 5 with
 * {@code (wParam & 0xFF) == MK_XBUTTON1}, but that byte also carries the Shift / Ctrl / other-mouse-button flags.
 * So pressing side button 4 while holding Shift, Ctrl or a mouse button is reported as side button 5 (e.g. the
 * Drop key instead of Toggle Perspective). Releases are decoded correctly.
 * <p>
 * Minecraft's and GuiScreen's {@code Mouse.next()} loops call {@link #next()} instead (see MixinMinecraft /
 * MixinGuiScreen): when a "button 5" press comes in, the real button state is read from Windows and the event is
 * turned back into button 4 if that is the one actually pressed.
 */
public final class MouseFix {
    private MouseFix() {
    }

    private interface User32 extends Library {
        short GetAsyncKeyState(int vKey);
    }

    private static final int VK_XBUTTON1 = 0x05, VK_XBUTTON2 = 0x06;
    private static boolean ready, failed;
    private static User32 user32;
    private static Field eventButton, buttons;

    /** True on Windows once the Win32 call and the LWJGL fields are reachable. */
    public static boolean available() {
        return init();
    }

    private static boolean init() {
        if (ready || failed) return ready;
        try {
            if (!System.getProperty("os.name", "").startsWith("Windows")) throw new IllegalStateException("not Windows");
            user32 = (User32) Native.loadLibrary("user32", User32.class);
            eventButton = Mouse.class.getDeclaredField("eventButton");
            eventButton.setAccessible(true);
            buttons = Mouse.class.getDeclaredField("buttons");
            buttons.setAccessible(true);
            ready = true;
        } catch (Throwable t) {
            failed = true;
            MLCore.LOG.info("Mouse side button fix disabled: {}", t.toString());
        }
        return ready;
    }

    public static boolean next() {
        if (!Mouse.next()) return false;
        int b = Mouse.getEventButton();
        if ((b == 3 || b == 4) && Mouse.getEventButtonState() && init()) {
            try {
                // Read both every time so the "pressed since last call" bits never go stale.
                short x1 = user32.GetAsyncKeyState(VK_XBUTTON1), x2 = user32.GetAsyncKeyState(VK_XBUTTON2);
                if (b == 4 && isButton1(x1, x2)) {
                    eventButton.setInt(null, 3);
                    ByteBuffer state = (ByteBuffer) buttons.get(null);
                    state.put(3, (byte) 1);
                    state.put(4, (byte) ((x2 & 0x8000) != 0 ? 1 : 0));
                }
            } catch (Throwable t) {
                failed = true;
                ready = false;
                MLCore.LOG.warn("Mouse side button fix failed, disabling it", t);
            }
        }
        return true;
    }

    private static boolean isButton1(short x1, short x2) {
        boolean down1 = (x1 & 0x8000) != 0, down2 = (x2 & 0x8000) != 0;
        if (down1 != down2) return down1;
        // Both up (a very quick click already released) or both held: use "pressed since the last check".
        return (x1 & 1) != 0 && (x2 & 1) == 0;
    }
}
