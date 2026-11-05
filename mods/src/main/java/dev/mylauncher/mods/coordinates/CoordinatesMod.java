package dev.mylauncher.mods.coordinates;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.TextHud;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod(modid = "ml_coordinates", name = "Coordinates", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class CoordinatesMod extends TextHud {
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    @Setting(name = "Show X")
    public boolean showX = true;
    @Setting(name = "Show Y")
    public boolean showY = true;
    @Setting(name = "Show Z")
    public boolean showZ = true;
    @Setting(name = "Show Direction")
    public boolean showDirection = true;
    @Setting(name = "Show Biome")
    public boolean showBiome = false;
    @Setting(name = "Show Chunk")
    public boolean showChunk = false;
    @Setting(name = "Single Line")
    public boolean singleLine = false;
    @Setting(name = "Decimals", min = 0, max = 3, step = 1)
    public int decimals = 0;

    public CoordinatesMod() {
        super("coordinates", "Coordinates", "Displays your coordinates on the HUD.", Category.HUD, false, 0f, 0.3f);
        fixedWidth = false;
    }

    private String num(double v) {
        return decimals == 0 ? String.valueOf(MathHelper.floor_double(v)) : String.format("%." + decimals + "f", v);
    }

    public static String direction(float yaw) {
        int i = MathHelper.floor_double((yaw * 8.0 / 360.0) + 0.5) & 7;
        return DIRS[i];
    }

    @Override
    protected List<String> lines() {
        List<String> out = new ArrayList<String>();
        double x = mc.thePlayer.posX, y = mc.thePlayer.getEntityBoundingBox().minY, z = mc.thePlayer.posZ;
        if (singleLine) {
            StringBuilder sb = new StringBuilder();
            if (showX) sb.append(num(x)).append(", ");
            if (showY) sb.append(num(y)).append(", ");
            if (showZ) sb.append(num(z)).append(", ");
            if (sb.length() > 0) sb.setLength(sb.length() - 2);
            if (showDirection) sb.append(" ").append(direction(mc.thePlayer.rotationYaw));
            out.add(sb.toString());
        } else {
            if (showX) out.add("X: " + num(x));
            if (showY) out.add("Y: " + num(y));
            if (showZ) out.add("Z: " + num(z));
            if (showDirection) out.add("Facing: " + direction(mc.thePlayer.rotationYaw));
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (showBiome && mc.theWorld.isBlockLoaded(pos)) {
            out.add("Biome: " + mc.theWorld.getChunkFromBlockCoords(pos).getBiome(pos, mc.theWorld.getWorldChunkManager()).biomeName);
        }
        if (showChunk) out.add("Chunk: " + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4));
        return out;
    }

    @Override
    protected List<String> sampleLines() {
        return singleLine ? Arrays.asList("120, 64, -340 N") : Arrays.asList("X: 120", "Y: 64", "Z: -340", "Facing: N");
    }
}
