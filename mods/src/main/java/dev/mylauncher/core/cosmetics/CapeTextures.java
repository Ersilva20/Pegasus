package dev.mylauncher.core.cosmetics;

import dev.mylauncher.core.MLCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Original Pegasus capes, painted in code as HD pixel art (512x256 texture = 80x128 visible canvas).
 * Standard cape layout, so they also work with the Physics mod.
 * Animated capes are repainted while someone is wearing them.
 */
public final class CapeTextures {
    private static final int S = 8;
    private static final int W = 64 * S, H = 32 * S;
    static final int FW = 10 * S, FH = 16 * S; // 80 x 128 canvas

    private static final class Entry {
        DynamicTexture texture;
        ResourceLocation location;
        long lastUsed;
        boolean animated;
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

    private static File configDir() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/pegasus");
    }
    private int[] logo;
    private int logoSize;

    public ResourceLocation get(String id) {
        if (id == null || Catalog.NONE.equals(id)) return null;
        Entry e = entries.get(id);
        if (e == null) {
            if (id.startsWith("file:")) {
                loadFileAsync(id);
                return null; // shows up as soon as it's loaded
            }
            e = create(id);
            if (e == null) return null;
            entries.put(id, e);
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
        if (!id.startsWith("file:")) return get(id);
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
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static String safeName(String id) {
        return id.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    public void tick(float time) {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Entry> me = it.next();
            if (me.getKey().startsWith("file:") && now - me.getValue().lastUsed > 30000) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(me.getValue().location);
                it.remove();
            }
        }
        for (Map.Entry<String, Entry> me : entries.entrySet()) {
            Entry e = me.getValue();
            if (!e.animated || now - e.lastUsed > 1000) continue;
            paint(me.getKey(), e.texture.getTextureData(), time);
            e.texture.updateDynamicTexture();
        }
    }

    private Entry create(String id) {
        Minecraft mc = Minecraft.getMinecraft();
        Entry e = new Entry();
        {
            boolean known = false;
            for (Catalog.Item item : Catalog.CAPES) {
                if (item.id.equals(id)) {
                    known = true;
                    e.animated = item.animated;
                }
            }
            if (!known) return null;
            e.texture = new DynamicTexture(W, H);
            paint(id, e.texture.getTextureData(), 0);
            e.texture.updateDynamicTexture();
        }
        e.location = mc.getTextureManager().getDynamicTextureLocation("pegasus_cape_" + id.replaceAll("[^a-z0-9_]", "_"), e.texture);
        return e;
    }

    // ================================================================ painting

    private interface Pattern {
        int color(int x, int y, float t);
    }

    private void paint(String id, int[] px, float t) {
        Pattern p = pattern(id);
        int lining = darker(p.color(FW / 2, FH / 3, t), 0.35f);
        int edge = darker(lining, 0.7f);
        for (int i = 0; i < px.length; i++) px[i] = edge;
        for (int y = 0; y < FH; y++) {
            for (int x = 0; x < FW; x++) {
                int c = p.color(x, y, t);
                // 2px dark frame + 1px highlight, like a stitched hem
                if (x < 2 || y < 2 || x >= FW - 2 || y >= FH - 2) c = darker(c, 0.35f);
                else if (x == 2 || y == 2 || x == FW - 3 || y == FH - 3) c = lighter(c, 0.25f);
                px[(S + y) * W + S + x] = c;
                px[(S + y) * W + 12 * S + x] = ((x / 4 + y / 4) % 2 == 0) ? lining : darker(lining, 0.85f);
            }
        }
    }

    private Pattern pattern(String id) {
        switch (id) {
            case "pegasus_light": return this::pegasusLight;
            case "aurora": return this::aurora;
            case "galaxy": return this::galaxy;
            case "synthwave": return this::synthwave;
            case "ocean": return this::ocean;
            case "flame": return this::flame;
            case "sakura": return this::sakura;
            case "ender": return this::ender;
            case "royal": return this::royal;
            case "storm": return this::storm;
            case "matrix": return this::matrix;
            case "frost": return this::frost;
            case "rainbow": return (x, y, t) -> hsb((((x + y) / 6) * 6 / (float) (FW + FH) - t * 0.01f) % 1f + 1f, 0.75f, 1f);
            case "checker": return (x, y, t) -> ((x / 10 + y / 10) % 2 == 0) ? 0xFF15121A : 0xFFF3E9D2;
            default: return this::pegasus;
        }
    }

    // ---------------------------------------------------------------- designs

    private int pegasus(int x, int y, float t) {
        int c = ditherGradient(0xFF060920, 0xFF1A1F5A, y / (float) FH, x, y);
        int h = hash(x, y);
        if (h % 113 == 0) c = 0xFFB8C2FF;
        if (y >= FH - 12 && y < FH - 4) return hsb(((x / 4) * 4) / (float) FW * 0.85f, 0.8f, 1f);
        int l = logoAt(x, y, 8, 18, 64);
        return l != 0 ? l : c;
    }

    private int pegasusLight(int x, int y, float t) {
        int c = ditherGradient(0xFFF4F7FF, 0xFFBFD0FF, y / (float) FH, x, y);
        float cloud = noise(x * 0.06f, y * 0.12f);
        if (y > FH - 34 && cloud > 0.45f) c = 0xFFFFFFFF;
        int l = logoAt(x, y, 8, 18, 64);
        return l != 0 ? l : c;
    }

    private int aurora(int x, int y, float t) {
        int c = ditherGradient(0xFF02070F, 0xFF0B2638, y / (float) FH, x, y);
        if (hash(x, y) % 97 == 0) c = blend(c, 0xFFFFFFFF, 0.7f);
        for (int band = 0; band < 2; band++) {
            float center = 34 + band * 22 + 9 * (float) Math.sin(x * 0.11 + t * 0.05 + band * 2) + 4 * (float) Math.sin(x * 0.27 - t * 0.03);
            float d = y - center;
            if (d > -2 && d < 26) {
                float a = d < 0 ? 0.9f : Math.max(0, 1 - d / 26f);
                int col = blend(band == 0 ? 0xFF35F0A0 : 0xFF6AD8FF, 0xFFB04CE0, Math.max(0, d) / 26f);
                c = blend(c, col, quant(a * 0.85f));
            }
        }
        float ridge = 104 + 10 * noise(x * 0.08f, 3f);
        if (y > ridge) c = y > ridge + 2 ? 0xFF050A10 : 0xFF0E1C26;
        return c;
    }

    private int galaxy(int x, int y, float t) {
        float n = noise(x * 0.05f, y * 0.05f) * 0.6f + noise(x * 0.11f + 7, y * 0.11f) * 0.4f;
        int c = ditherGradient(0xFF07031A, 0xFF2A0E52, quant(n), x, y);
        float m = noise(x * 0.07f + 20, y * 0.04f);
        if (m > 0.55f) c = blend(c, 0xFFE0569B, quant((m - 0.55f) * 2.2f));
        if (m < 0.3f) c = blend(c, 0xFF2D6BD6, quant((0.3f - m) * 2f));
        int h = hash(x, y);
        if (h % 61 == 0) c = blend(c, 0xFFFFFFFF, 0.5f + 0.5f * (float) Math.sin(t * 0.3f + h % 9));
        if (h % 977 == 0) c = 0xFFFFFFFF;
        return c;
    }

    private int synthwave(int x, int y, float t) {
        int horizon = 76;
        if (y < horizon) {
            int c = y < 40 ? ditherGradient(0xFF14042C, 0xFF5A1466, y / 40f, x, y)
                    : ditherGradient(0xFF5A1466, 0xFFFF4F8B, (y - 40) / 36f, x, y);
            if (y < 40 && hash(x, y) % 89 == 0) c = 0xFFFFD6F0;
            float dx = x - FW / 2f, dy = y - 58;
            if (dx * dx + dy * dy < 22 * 22) {
                boolean gap = y > 56 && ((y - 56) % 5 < (y - 56) / 6 + 1);
                if (!gap) c = ditherGradient(0xFFFFE45C, 0xFFFF4F8B, (y - 36) / 44f, x, y);
            }
            return c;
        }
        int c = ditherGradient(0xFF1A0530, 0xFF0A0216, (y - horizon) / (float) (FH - horizon), x, y);
        float depth = (y - horizon + 1) / (float) (FH - horizon);
        float scroll = (t * 0.6f) % 8f;
        boolean hLine = ((int) ((1f / depth) * 4 + scroll)) % 8 == 0;
        float xr = (x - FW / 2f) / Math.max(0.08f, depth);
        boolean vLine = Math.abs(xr % 14) < 1.2f / Math.max(0.3f, depth) * 0.6f;
        if (hLine || vLine || y == horizon) c = 0xFFFF3AD0;
        return c;
    }

    private int ocean(int x, int y, float t) {
        if (y < 48) {
            int c = ditherGradient(0xFF9BE3FF, 0xFFFFD9A8, y / 48f, x, y);
            float dx = x - 56, dy = y - 34;
            if (dx * dx + dy * dy < 81) c = 0xFFFFF2B0;
            return c;
        }
        int c = ditherGradient(0xFF2A9BE0, 0xFF06244F, (y - 48) / (float) (FH - 48), x, y);
        double wave = Math.sin(x * 0.35 + t * 0.25 + y * 0.9) + Math.sin(x * 0.13 - t * 0.12);
        if ((y % 6) < 1 && wave > 0.9) c = blend(c, 0xFFE8F8FF, 0.8f);
        if (Math.abs(x - 56) < 6 - (y - 48) / 16 && (y + (int) t) % 4 < 2) c = blend(c, 0xFFFFE9A0, 0.6f);
        return c;
    }

    private int flame(int x, int y, float t) {
        float fromBottom = (FH - y) / (float) FH;
        float n = noise(x * 0.12f, y * 0.08f + t * 0.35f) * 0.7f + noise(x * 0.3f, y * 0.2f + t * 0.6f) * 0.3f;
        float heat = 1.25f - fromBottom * 1.45f + (n - 0.5f) * 0.6f;
        if (heat < 0.12f) return ditherGradient(0xFF120605, 0xFF2A0C08, y / (float) FH, x, y);
        if (heat < 0.3f) return 0xFF6E160C;
        if (heat < 0.5f) return 0xFFC23A18;
        if (heat < 0.7f) return 0xFFF07A24;
        if (heat < 0.88f) return 0xFFFFC14A;
        return 0xFFFFF2B8;
    }

    private int sakura(int x, int y, float t) {
        int c = ditherGradient(0xFFFFE6F0, 0xFFF7A8C8, y / (float) FH, x, y);
        // branch from the top-left
        float by = 18 + x * 0.35f + 4 * (float) Math.sin(x * 0.15);
        if (x < 62 && Math.abs(y - by) < 1.6f - x / 60f) c = 0xFF5A3424;
        if (x < 62 && Math.abs(y - by) < 6 && hash(x / 3, y / 3) % 5 == 0) c = 0xFFFF7FB0;
        // falling petals
        for (int i = 0; i < 14; i++) {
            int seed = hash(i, 99);
            float pxp = (seed % FW + (float) Math.sin(t * 0.05f + i) * 6 + FW) % FW;
            float pyp = (seed / 7 % FH + t * (0.25f + (seed % 5) * 0.05f)) % FH;
            if (Math.abs(x - pxp) < 1.5f && Math.abs(y - pyp) < 1.0f) return 0xFFFF5C9A;
        }
        return c;
    }

    private int ender(int x, int y, float t) {
        int c = ditherGradient(0xFF0A0614, 0xFF241038, y / (float) FH, x, y);
        int h = hash(x, (y + (int) (t * 0.5f)) % FH);
        if (h % 71 == 0) c = 0xFFC86BFF;
        float dx = x - FW / 2f, dy = y - 52;
        float r = (float) Math.sqrt(dx * dx * 0.55f + dy * dy);
        if (r < 24) c = 0xFF0E3B33;
        if (r < 20) c = ditherGradient(0xFF3BE8B0, 0xFF0F6E5A, r / 20f, x, y);
        if (Math.abs(dx) < 3.5f && Math.abs(dy) < 13) c = 0xFF051410;
        return c;
    }

    private int royal(int x, int y, float t) {
        int c = ditherGradient(0xFF7A0E1E, 0xFF3A0610, y / (float) FH, x, y);
        int dx = Math.abs((x % 16) - 8), dy = Math.abs((y % 16) - 8);
        if (dx + dy == 8) c = 0xFFB8862A;
        if (x < 8 || x >= FW - 8) c = ((y / 4) % 2 == 0) ? 0xFFE0B040 : 0xFFB8862A;
        // crown emblem
        int cx = x - 26, cy = y - 30;
        if (cx >= 0 && cx < 28 && cy >= 0 && cy < 20) {
            boolean base = cy >= 12;
            boolean spike = cy < 12 && ((cx < 6 && cy > 2) || (cx >= 11 && cx < 17) || (cx >= 22 && cy > 2));
            if (base || spike) c = cy % 6 == 0 ? 0xFFFFE08A : 0xFFE6B13C;
            if (cy == 15 && (cx == 6 || cx == 14 || cx == 22)) c = 0xFF3AB0FF;
        }
        float shine = (x + y - (t * 1.5f) % (FW + FH));
        if (Math.abs(shine) < 2 && (c == 0xFFE6B13C || c == 0xFFB8862A)) c = 0xFFFFF4C8;
        return c;
    }

    private int storm(int x, int y, float t) {
        float n = noise(x * 0.06f + t * 0.02f, y * 0.06f);
        int c = ditherGradient(0xFF1C1F2A, 0xFF3D4458, quant(n), x, y);
        boolean flash = ((int) t / 20) % 3 == 0 && (t % 20) < 3;
        // lightning bolt zig-zag
        float bx = 46 - Math.abs(((y / 10) % 2) * 10 - 5) - y * 0.08f;
        if (y > 20 && y < 110 && Math.abs(x - bx) < 2) c = flash ? 0xFFFFFFFF : 0xFFF2E36A;
        if (flash) c = blend(c, 0xFFDCE6FF, 0.25f);
        return c;
    }

    private int matrix(int x, int y, float t) {
        int col = x / 5;
        int seed = hash(col, 7);
        float head = (seed % FH + t * (0.6f + (seed % 4) * 0.25f)) % (FH + 40);
        float d = head - y;
        int c = 0xFF020804;
        if (d >= 0 && d < 40) {
            boolean glyph = hash(x / 1, (y / 6) * 31 + col + (int) (t / 8)) % 3 != 0 && x % 5 != 4 && y % 6 != 5;
            if (glyph) c = d < 2 ? 0xFFD8FFD8 : blend(0xFF2BFF6A, 0xFF02260C, d / 40f);
        }
        return c;
    }

    private int frost(int x, int y, float t) {
        int c = ditherGradient(0xFFDDF5FF, 0xFF4E9ED8, y / (float) FH, x, y);
        // snowflakes: 7x7 crosses on a grid
        int gx = x % 26, gy = (y + 6) % 30;
        int sx = Math.abs(gx - 13), sy = Math.abs(gy - 15);
        if ((sx == 0 && sy < 5) || (sy == 0 && sx < 5) || (sx == sy && sx < 4)) c = 0xFFFFFFFF;
        int h = hash(x, (y - (int) (t * 0.4f) + FH * 4) % FH);
        if (h % 53 == 0) c = 0xFFFFFFFF;
        return c;
    }

    // ---------------------------------------------------------------- logo

    private int logoAt(int x, int y, int ox, int oy, int size) {
        int[] l = logo();
        if (l == null || x < ox || x >= ox + size || y < oy || y >= oy + size) return 0;
        int c = l[((y - oy) * logoSize / size) * logoSize + (x - ox) * logoSize / size];
        return ((c >>> 24) & 0xFF) > 110 ? (0xFF000000 | c) : 0;
    }

    private int[] logo() {
        if (logo != null) return logo.length == 0 ? null : logo;
        try (InputStream in = Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("mlcore", "textures/logo128.png")).getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            logoSize = img.getWidth();
            logo = img.getRGB(0, 0, logoSize, logoSize, null, 0, logoSize);
        } catch (Exception e) {
            logo = new int[0];
        }
        return logo.length == 0 ? null : logo;
    }

    // ---------------------------------------------------------------- helpers

    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    /** Pixel-art gradient: 8 color steps with Bayer dithering between them. */
    static int ditherGradient(int a, int b, float t, int x, int y) {
        t = Math.max(0, Math.min(1, t)) * 8f;
        int step = (int) t;
        float frac = t - step;
        if (frac * 16 > BAYER[(y & 3) * 4 + (x & 3)]) step++;
        return blend(a, b, Math.min(8, step) / 8f);
    }

    static float quant(float v) {
        return Math.round(Math.max(0, Math.min(1, v)) * 6) / 6f;
    }

    static float noise(float x, float y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float xf = x - xi, yf = y - yi;
        float u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf);
        float a = (hash(xi, yi) % 1000) / 1000f, b = (hash(xi + 1, yi) % 1000) / 1000f;
        float c = (hash(xi, yi + 1) % 1000) / 1000f, d = (hash(xi + 1, yi + 1) % 1000) / 1000f;
        return (a + (b - a) * u) * (1 - v) + (c + (d - c) * u) * v;
    }

    static int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7FFFFFFF;
    }

    static int hsb(float h, float s, float b) {
        return 0xFF000000 | (Color.HSBtoRGB(h, s, b) & 0xFFFFFF);
    }

    static int darker(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int lighter(int c, float f) {
        return blend(c, 0xFFFFFFFF, f);
    }

    static int blend(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
