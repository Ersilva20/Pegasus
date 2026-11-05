package dev.mylauncher.mods.mumblelink;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Positional audio for Mumble (Windows). Enable "Link to Game and Transmit Position" in Mumble; players on
 * the same server who also use Mumble Link will hear each other from the direction they are in.
 * Writes Mumble's shared-memory "LinkedMem" structure every tick.
 */
@Mod(modid = "ml_mumblelink", name = "Mumble Link", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MumbleLinkMod extends Feature {
    interface Kernel32 extends Library {
        Pointer OpenFileMappingW(int access, boolean inherit, WString name);

        Pointer MapViewOfFile(Pointer mapping, int access, int offsetHigh, int offsetLow, int bytes);
    }

    private static final int FILE_MAP_ALL_ACCESS = 0xF001F;
    private static final int SIZE = 5460;

    @Setting(name = "Include Server in Context", description = "Only players on the same server hear you positionally")
    public boolean serverContext = true;

    private Kernel32 kernel;
    private Pointer mem;
    private long nextAttempt;
    private int tick;
    private boolean unsupported;

    public MumbleLinkMod() {
        super("mumblelink", "Mumble Link", "Uses mumble to create positional audio in the Minecraft world.", Category.UTILITY);
    }

    private boolean connect() {
        if (mem != null) return true;
        if (unsupported || System.currentTimeMillis() < nextAttempt) return false;
        nextAttempt = System.currentTimeMillis() + 5000;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            unsupported = true;
            MLCore.LOG.info("Mumble Link currently supports Windows only");
            return false;
        }
        try {
            if (kernel == null) kernel = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
            Pointer handle = kernel.OpenFileMappingW(FILE_MAP_ALL_ACCESS, false, new WString("MumbleLink"));
            if (handle == null) return false;
            mem = kernel.MapViewOfFile(handle, FILE_MAP_ALL_ACCESS, 0, 0, SIZE);
            return mem != null;
        } catch (Throwable t) {
            unsupported = true;
            MLCore.LOG.warn("Mumble Link unavailable", t);
            return false;
        }
    }

    private static void wide(Pointer p, long offset, String s, int maxChars) {
        int n = Math.min(s.length(), maxChars - 1);
        for (int i = 0; i < n; i++) p.setChar(offset + i * 2L, s.charAt(i));
        p.setChar(offset + n * 2L, '\0');
    }

    private static void vec(Pointer p, long offset, double x, double y, double z) {
        p.setFloat(offset, (float) x);
        p.setFloat(offset + 4, (float) y);
        p.setFloat(offset + 8, (float) z);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null || !connect()) return;
        try {
            Vec3 look = mc.thePlayer.getLookVec();
            double x = mc.thePlayer.posX, y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), z = mc.thePlayer.posZ;
            if (mem.getInt(0) != 2) {
                mem.setInt(0, 2);
                wide(mem, 44, "Minecraft", 256);
                wide(mem, 1364, "Minecraft 1.8.9 (Pegasus Mumble Link)", 2048);
            }
            mem.setInt(4, ++tick);
            // Minecraft is right-handed with +Z south; Mumble is left-handed, so mirror the Z axis.
            vec(mem, 8, x, y, -z);
            vec(mem, 20, look.xCoord, look.yCoord, -look.zCoord);
            vec(mem, 32, 0, 1, 0);
            vec(mem, 556, x, y, -z);
            vec(mem, 568, look.xCoord, look.yCoord, -look.zCoord);
            vec(mem, 580, 0, 1, 0);
            wide(mem, 592, mc.thePlayer.getName(), 256);
            String ctx = serverContext && mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP.toLowerCase()
                    : "mylauncher-" + (mc.isSingleplayer() ? "sp" : "mp");
            byte[] bytes = ctx.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int len = Math.min(bytes.length, 256);
            mem.setInt(1104, len);
            mem.write(1108, bytes, 0, len);
        } catch (Throwable t) {
            mem = null;
        }
    }
}
