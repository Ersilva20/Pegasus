package dev.mylauncher.mods.waypoints;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Commands: /waypoint add &lt;name&gt; [x y z], /waypoint remove &lt;name&gt;, /waypoint list,
 * /waypoint toggle &lt;name&gt;, /waypoint color &lt;name&gt; &lt;hex&gt;. Key B adds one at your position.
 * Waypoints are saved per server/world and dimension in config/pegasus/waypoints_data.json.
 */
@Mod(modid = "ml_waypoints", name = "Waypoints", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class WaypointsMod extends Feature {
    public static class Waypoint {
        public String name;
        public String world;
        public int dimension;
        public int x, y, z;
        public int color = 0xFF55FFFF;
        public boolean visible = true;
    }

    public static class Data {
        public List<Waypoint> waypoints = new ArrayList<Waypoint>();
    }

    @Setting(name = "Show Beams")
    public boolean beams = true;
    @Setting(name = "Show Labels")
    public boolean labels = true;
    @Setting(name = "Show Distance")
    public boolean distance = true;
    @Setting(name = "Label Scale", min = 0.5, max = 3, step = 0.1)
    public float labelScale = 1.2f;
    @Setting(name = "Max Render Distance", min = 50, max = 10000, step = 50)
    public int maxDistance = 5000;

    private final KeyBinding addKey = MLCore.key("Add Waypoint Here", Keyboard.KEY_B);
    private Data data;
    private boolean commandRegistered;

    public WaypointsMod() {
        super("waypoints", "Waypoints", "Create waypoints anywhere in the world with distance, name and a beam.", Category.WORLD, true);
    }

    private Data data() {
        if (data == null) data = Config.readJson("waypoints_data.json", Data.class, new Data());
        return data;
    }

    private void save() {
        Config.writeJson("waypoints_data.json", data());
    }

    private String worldKey() {
        if (mc.isSingleplayer() && mc.getIntegratedServer() != null) return "sp:" + mc.getIntegratedServer().getFolderName();
        return mc.getCurrentServerData() != null ? "mp:" + mc.getCurrentServerData().serverIP.toLowerCase() : "unknown";
    }

    private List<Waypoint> current() {
        List<Waypoint> out = new ArrayList<Waypoint>();
        if (mc.thePlayer == null) return out;
        String w = worldKey();
        for (Waypoint p : data().waypoints) if (p.world.equals(w) && p.dimension == mc.thePlayer.dimension) out.add(p);
        return out;
    }

    private Waypoint find(String name) {
        for (Waypoint p : current()) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    public void add(String name, int x, int y, int z) {
        Waypoint p = new Waypoint();
        p.name = name;
        p.world = worldKey();
        p.dimension = mc.thePlayer.dimension;
        p.x = x;
        p.y = y;
        p.z = z;
        int[] palette = {0xFF55FFFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFFF55, 0xFFFF55FF, 0xFFFFAA00};
        p.color = palette[data().waypoints.size() % palette.length];
        data().waypoints.add(p);
        save();
        MLCore.chat("Added waypoint " + EnumChatFormatting.YELLOW + name + EnumChatFormatting.RESET + " at " + x + ", " + y + ", " + z);
    }

    @Override
    protected void onEnable() {
        if (commandRegistered) return;
        commandRegistered = true;
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return "waypoint";
            }

            @Override
            public List<String> getCommandAliases() {
                List<String> a = new ArrayList<String>();
                a.add("wp");
                return a;
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/waypoint <add|remove|list|toggle|color> ...";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                if (!isEnabled()) {
                    MLCore.chat("Waypoints mod is disabled.");
                    return;
                }
                if (args.length == 0) {
                    MLCore.chat(getCommandUsage(sender));
                    return;
                }
                String sub = args[0].toLowerCase();
                if (sub.equals("add") && args.length >= 2) {
                    if (args.length >= 5) {
                        try {
                            add(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                        } catch (NumberFormatException e) {
                            MLCore.chat("Coordinates must be numbers.");
                        }
                    } else {
                        add(args[1], (int) Math.floor(mc.thePlayer.posX), (int) Math.floor(mc.thePlayer.posY), (int) Math.floor(mc.thePlayer.posZ));
                    }
                } else if (sub.equals("remove") && args.length >= 2) {
                    Waypoint p = find(args[1]);
                    if (p != null) {
                        data().waypoints.remove(p);
                        save();
                        MLCore.chat("Removed " + p.name);
                    } else MLCore.chat("No waypoint named " + args[1]);
                } else if (sub.equals("toggle") && args.length >= 2) {
                    Waypoint p = find(args[1]);
                    if (p != null) {
                        p.visible = !p.visible;
                        save();
                        MLCore.chat(p.name + (p.visible ? " shown" : " hidden"));
                    }
                } else if (sub.equals("color") && args.length >= 3) {
                    Waypoint p = find(args[1]);
                    if (p != null) {
                        try {
                            p.color = 0xFF000000 | Integer.parseInt(args[2].replace("#", ""), 16);
                            save();
                        } catch (NumberFormatException e) {
                            MLCore.chat("Use a hex color like FF5555");
                        }
                    }
                } else if (sub.equals("list")) {
                    List<Waypoint> list = current();
                    if (list.isEmpty()) MLCore.chat("No waypoints in this world.");
                    for (Waypoint p : list) MLCore.chat(p.name + ": " + p.x + ", " + p.y + ", " + p.z + (p.visible ? "" : " (hidden)"));
                } else {
                    MLCore.chat(getCommandUsage(sender));
                }
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

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (addKey.isPressed() && mc.thePlayer != null) {
            add("Waypoint " + (current().size() + 1), (int) Math.floor(mc.thePlayer.posX), (int) Math.floor(mc.thePlayer.posY), (int) Math.floor(mc.thePlayer.posZ));
        }
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (mc.thePlayer == null) return;
        double vx = mc.getRenderManager().viewerPosX, vy = mc.getRenderManager().viewerPosY, vz = mc.getRenderManager().viewerPosZ;
        for (Waypoint p : current()) {
            if (!p.visible) continue;
            double dx = p.x + 0.5 - vx, dy = p.y - vy, dz = p.z + 0.5 - vz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > maxDistance) continue;
            if (beams) {
                RenderUtil.beginWorldLines(1f);
                GlStateManager.enableDepth();
                RenderUtil.boxFilled(new AxisAlignedBB(dx - 0.15, -vy, dz - 0.15, dx + 0.15, 256 - vy, dz + 0.15), (p.color & 0xFFFFFF) | 0x50000000);
                RenderUtil.endWorldLines();
            }
            if (labels) {
                // Keep far labels readable by pulling them closer to the camera.
                double scaleDist = Math.min(dist, 12);
                double f = dist == 0 ? 1 : scaleDist / dist;
                String text = p.name + (distance ? " §7[" + Math.round(dist) + "m]" : "");
                RenderUtil.worldLabel(text, dx * f, (dy + 1.5) * f, dz * f, p.color, labelScale, true);
            }
        }
    }
}
