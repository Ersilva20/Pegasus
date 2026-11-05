package dev.mylauncher.mods.customskies;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Config;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Put a PNG in .minecraft/config/pegasus/skies/ and type its name in settings.
 * A 3:2 image is read as a cube map (top row: down, up, north / bottom row: east, south, west);
 * any other image is used on all six sides.
 */
@Mod(modid = "ml_customskies", name = "Custom Skies", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class CustomSkiesMod extends Feature {
    @Setting(name = "Sky Image File (in config/pegasus/skies)")
    public String file = "sky.png";
    @Setting(name = "Overworld Only")
    public boolean overworldOnly = true;

    private ResourceLocation texture;
    private boolean cubeLayout;
    private String loadedFile;
    private WorldClient appliedTo;
    private final SkyRenderer renderer = new SkyRenderer();

    public CustomSkiesMod() {
        super("customskies", "Custom Skies", "Use the sky texture from your resource pack.", Category.WORLD);
    }

    private File skiesDir() {
        File d = new File(Config.dir(), "skies");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    private void load() {
        loadedFile = file;
        texture = null;
        File f = new File(skiesDir(), file);
        if (!f.isFile()) {
            MLCore.LOG.info("Custom sky {} not found in {}", file, skiesDir());
            return;
        }
        try {
            BufferedImage img = ImageIO.read(f);
            cubeLayout = img.getWidth() * 2 == img.getHeight() * 3;
            texture = mc.getTextureManager().getDynamicTextureLocation("ml_sky", new DynamicTexture(img));
        } catch (Exception e) {
            MLCore.LOG.error("Could not load sky " + f, e);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null) return;
        if (!file.equals(loadedFile)) load();
        boolean want = texture != null && (!overworldOnly || mc.theWorld.provider.getDimensionId() == 0);
        if (want && (appliedTo != mc.theWorld || mc.theWorld.provider.getSkyRenderer() != renderer)) {
            mc.theWorld.provider.setSkyRenderer(renderer);
            appliedTo = mc.theWorld;
        } else if (!want && mc.theWorld.provider.getSkyRenderer() == renderer) {
            mc.theWorld.provider.setSkyRenderer(null);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.theWorld != null && mc.theWorld.provider.getSkyRenderer() == renderer) mc.theWorld.provider.setSkyRenderer(null);
        appliedTo = null;
    }

    private final class SkyRenderer extends IRenderHandler {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
            if (texture == null) return;
            GlStateManager.disableFog();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.depthMask(false);
            mc.getTextureManager().bindTexture(texture);
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            for (int face = 0; face < 6; face++) {
                GlStateManager.pushMatrix();
                switch (face) {
                    case 1: GlStateManager.rotate(180, 1, 0, 0); break; // up
                    case 2: GlStateManager.rotate(90, 1, 0, 0); break; // north
                    case 3: GlStateManager.rotate(90, 1, 0, 0); GlStateManager.rotate(-90, 0, 0, 1); break; // east
                    case 4: GlStateManager.rotate(-90, 1, 0, 0); GlStateManager.rotate(180, 0, 1, 0); break; // south
                    case 5: GlStateManager.rotate(90, 1, 0, 0); GlStateManager.rotate(90, 0, 0, 1); break; // west
                    default: break; // down
                }
                float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
                if (cubeLayout) {
                    u0 = (face % 3) / 3f;
                    v0 = (face / 3) / 2f;
                    u1 = u0 + 1 / 3f;
                    v1 = v0 + 0.5f;
                }
                w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                w.pos(-100, -100, -100).tex(u0, v0).endVertex();
                w.pos(-100, -100, 100).tex(u0, v1).endVertex();
                w.pos(100, -100, 100).tex(u1, v1).endVertex();
                w.pos(100, -100, -100).tex(u1, v0).endVertex();
                t.draw();
                GlStateManager.popMatrix();
            }
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
        }
    }
}
