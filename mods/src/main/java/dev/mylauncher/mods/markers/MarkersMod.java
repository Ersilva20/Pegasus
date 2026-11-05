package dev.mylauncher.mods.markers;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Press the Marker key (middle mouse by default) to ping the block you look at. Optionally shares it
 * in party chat as "[Marker] x y z"; markers in that format from teammates are shown too.
 */
@Mod(modid = "ml_markers", name = "Ping Markers", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MarkersMod extends Feature {
    private static final Pattern SHARED = Pattern.compile("\\[Marker\\] (-?\\d+) (-?\\d+) (-?\\d+)");

    private static final class Marker {
        BlockPos pos;
        String owner;
        long expires;
    }

    @Setting(name = "Duration (seconds)", min = 3, max = 60, step = 1)
    public int duration = 10;
    @Setting(name = "Share in Party Chat")
    public boolean share = false;
    @Setting(name = "Party Chat Command")
    public String command = "/pc";
    @Setting(name = "Show Teammates' Markers")
    public boolean receive = true;
    @Setting(name = "Color", color = true)
    public int color = 0xFFFFAA00;

    private final KeyBinding key = MLCore.key("Place Marker", -98);
    private final List<Marker> markers = new ArrayList<Marker>();

    public MarkersMod() {
        super("markers", "Ping Markers", "Press a key to mark the block you look at, and share it in party chat if you want.", Category.PVP);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        check();
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        check();
    }

    private void check() {
        if (!key.isPressed() || mc.thePlayer == null) return;
        MovingObjectPosition mop = mc.thePlayer.rayTrace(200, 1f);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        BlockPos pos = mop.getBlockPos();
        place(pos, "You");
        if (share) mc.thePlayer.sendChatMessage(command + " [Marker] " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }

    private void place(BlockPos pos, String owner) {
        Marker m = new Marker();
        m.pos = pos;
        m.owner = owner;
        m.expires = System.currentTimeMillis() + duration * 1000L;
        markers.add(m);
        mc.thePlayer.playSound("random.orb", 0.5f, 1.5f);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!receive || event.message == null) return;
        String text = event.message.getUnformattedText();
        if (text.contains(mc.getSession().getUsername())) return;
        Matcher m = SHARED.matcher(text);
        if (m.find()) {
            place(new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))), "Team");
        }
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        long now = System.currentTimeMillis();
        for (Iterator<Marker> it = markers.iterator(); it.hasNext(); ) if (it.next().expires < now) it.remove();
        if (markers.isEmpty() || mc.thePlayer == null) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        for (Marker m : markers) {
            AxisAlignedBB bb = new AxisAlignedBB(m.pos, m.pos.add(1, 1, 1)).offset(-vx, -vy, -vz).expand(0.01, 0.01, 0.01);
            RenderUtil.beginWorldLines(3f);
            RenderUtil.boxFilled(bb, (color & 0xFFFFFF) | 0x40000000);
            RenderUtil.boxOutline(bb, color);
            GlStateManager.enableDepth();
            RenderUtil.endWorldLines();
            double dx = m.pos.getX() + 0.5 - vx, dy = m.pos.getY() + 1.6 - vy, dz = m.pos.getZ() + 0.5 - vz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double f = dist > 12 ? 12 / dist : 1;
            RenderUtil.worldLabel("▼ " + m.owner + " §7" + Math.round(dist) + "m", dx * f, dy * f, dz * f, color, 1.2f, true);
        }
    }
}
