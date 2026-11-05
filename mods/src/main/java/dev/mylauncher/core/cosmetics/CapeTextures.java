package dev.mylauncher.core.cosmetics;

import dev.mylauncher.core.MLCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Textures of the PNG capes in config/pegasus/cosmetics/capes (standard cape layout, any 2:1 size, so they also
 * work with the Physics mod). Loaded off-thread, and freed again when nobody has worn them for 30 seconds.
 */
public final class CapeTextures {
    private static final class Entry {
        DynamicTexture texture;
        ResourceLocation location;
        long lastUsed;
    }

    private final Map<String, Entry> entries = new HashMap<String, Entry>();
    private final Map<String, ResourceLocation> thumbs = new HashMap<String, ResourceLocation>();
    private final Set<String> loading = ConcurrentHashMap.newKeySet();
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Pegasus-CapeLoader");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    static File configDir() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/pegasus");
    }

    public ResourceLocation get(String id) {
        if (id == null || !id.startsWith("file:")) return null;
        Entry e = entries.get(id);
        if (e == null) {
            loadFileAsync(id);
            return null; // shows up as soon as it's loaded
        }
        e.lastUsed = System.currentTimeMillis();
        return e.location;
    }

    /** Big PNGs (HD capes can be 2048x1024) are decoded off-thread and shrunk to 512 wide. */
    private void loadFileAsync(final String id) {
        if (!loading.add(id)) return;
        final File f = Catalog.capeFile(configDir(), id);
        if (f == null) {
            loading.remove(id);
            return;
        }
        LOADER.submit(() -> {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) throw new IllegalStateException("not an image");
                final BufferedImage scaled = scale(img, Math.min(img.getWidth(), 512), Math.min(img.getWidth(), 512) * img.getHeight() / img.getWidth());
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Entry e = new Entry();
                    e.texture = new DynamicTexture(scaled);
                    e.location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("pegasus_cape_" + safeName(id), e.texture);
                    e.lastUsed = System.currentTimeMillis();
                    entries.put(id, e);
                    loading.remove(id);
                });
            } catch (Exception ex) {
                MLCore.LOG.warn("Could not load cape {}: {}", f, ex.toString());
            }
        });
    }

    /** Small preview of the cape's back (40x64) for Wardrobe cards, cached on disk in capes/.thumbs. */
    public ResourceLocation thumbnail(final String id) {
        if (!id.startsWith("file:")) return null;
        ResourceLocation rl = thumbs.get(id);
        if (rl != null || !loading.add("thumb:" + id)) return rl;
        final File f = Catalog.capeFile(configDir(), id);
        if (f == null) return null;
        LOADER.submit(() -> {
            try {
                File cacheDir = new File(Catalog.customCapeDir(configDir()), ".thumbs");
                cacheDir.mkdirs();
                File cache = new File(cacheDir, safeName(id) + ".png");
                BufferedImage thumb = null;
                if (cache.isFile() && cache.lastModified() >= f.lastModified()) thumb = ImageIO.read(cache);
                if (thumb == null) {
                    BufferedImage img = ImageIO.read(f);
                    int w = img.getWidth(), h = img.getHeight();
                    BufferedImage front = img.getSubimage(w / 64, h / 32, Math.max(1, 10 * w / 64), Math.max(1, 16 * h / 32));
                    thumb = scale(front, 40, 64);
                    ImageIO.write(thumb, "png", cache);
                }
                final BufferedImage done = thumb;
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    thumbs.put(id, Minecraft.getMinecraft().getTextureManager()
                            .getDynamicTextureLocation("pegasus_thumb_" + safeName(id), new DynamicTexture(done)));
                    loading.remove("thumb:" + id);
                });
            } catch (Exception ex) {
                MLCore.LOG.warn("Could not make thumbnail for {}: {}", f, ex.toString());
            }
        });
        return null;
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h && src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        // Classic 64x32 capes stay crisp; HD capes are smoothed when shrunk.
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, w >= src.getWidth()
                ? java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static String safeName(String id) {
        return id.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    /** Frees capes nobody has worn for 30 seconds. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Entry> me = it.next();
            if (now - me.getValue().lastUsed > 30000) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(me.getValue().location);
                it.remove();
            }
        }
    }
}
