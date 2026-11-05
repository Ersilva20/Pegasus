package dev.mylauncher.core.cosmetics;

import dev.mylauncher.core.gui.Pixel;
import dev.mylauncher.core.gui.SettingsScreen;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Wardrobe: live 3D preview on the left (drag to rotate), cape grid on the right with a search box and
 * one tab per pack (built-in, each folder in capes/, loose files).
 */
public class WardrobeScreen extends GuiScreen {
    private static final int CARD_W = 60, CARD_H = 70, GAP = 5;
    private static final String ALL = "ALL";
    private static String lastPack = ALL;

    private final GuiScreen parent;
    private final CosmeticsMod mod;
    private String pack = lastPack;
    private float yaw = 160f; // start looking at the back, where the cape is
    private boolean dragging;
    private int lastMouseX;
    private int scroll;
    private int px, py, pw, ph;
    private GuiTextField search;
    private List<Catalog.Item> all = new ArrayList<Catalog.Item>();
    private List<Catalog.Item> shown = new ArrayList<Catalog.Item>();
    private final List<String> packs = new ArrayList<String>();
    private String lastQuery = "";

    public WardrobeScreen(GuiScreen parent, CosmeticsMod mod) {
        this.parent = parent;
        this.mod = mod;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(width - 12, 480);
        ph = Math.min(height - 12, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        search = new GuiTextField(0, fontRendererObj, gridX() + 4, py + 24, 112, 10);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(40);
        search.setText(lastQuery);
        reload();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        lastPack = pack;
    }

    private File configDir() {
        return new File(mc.mcDataDir, "config/pegasus");
    }

    /** Re-reads the catalog (folders are scanned once per open, not every frame). */
    private void reload() {
        all = new ArrayList<Catalog.Item>(Catalog.capesWithCustom(configDir()));
        Set<String> p = new LinkedHashSet<String>();
        p.add(ALL);
        for (Catalog.Item i : all) p.add(i.pack);
        packs.clear();
        packs.addAll(p);
        if (!packs.contains(pack)) pack = ALL;
        filter();
    }

    private void filter() {
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        lastQuery = q;
        shown = new ArrayList<Catalog.Item>();
        for (Catalog.Item i : all) {
            if (!ALL.equals(pack) && !pack.equals(i.pack)) continue;
            if (!q.isEmpty() && !i.name.toLowerCase().contains(q)) continue;
            shown.add(i);
        }
        scroll = 0;
    }

    private int gridX() {
        return px + 150;
    }

    private int gridY() {
        return py + 60;
    }

    private int viewH() {
        return py + ph - 6 - gridY();
    }

    private int cols() {
        return Math.max(1, (px + pw - 8 - gridX() + GAP) / (CARD_W + GAP));
    }

    private int total() {
        return shown.size() + 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Pixel.overlay(width, height);
        String meta = all.size() + " CAPES";
        Pixel.window(px, py - 2, px + pw, py + ph, "WARDROBE", meta);

        // Preview
        int vx = px + 8, vy = py + 22, vw = 134, vh = ph - 54;
        Pixel.well(vx, vy, vx + vw, vy + vh);
        if (dragging) {
            if (Mouse.isButtonDown(0)) {
                yaw += (mouseX - lastMouseX) * 2f;
                lastMouseX = mouseX;
            } else {
                dragging = false;
            }
        }
        GlStateManager.color(1, 1, 1, 1);
        if (mc.thePlayer != null) drawPlayer(vx + vw / 2, vy + vh - 18, Math.min(62, vh / 3), yaw, mc.thePlayer);
        drawCenteredString(fontRendererObj, "DRAG TO ROTATE", vx + vw / 2, vy + vh - 11, Pixel.DIM);
        Pixel.button(vx, py + ph - 26, 64, 18, "FRONT", Pixel.Style.GRAY, mouseX, mouseY);
        Pixel.button(vx + 70, py + ph - 26, 64, 18, "BACK", Pixel.Style.GRAY, mouseX, mouseY);

        // Search, pack chips and options
        Pixel.button(px + pw - 66, py + 19, 58, 18, "OPTIONS", Pixel.Style.COPPER, mouseX, mouseY);
        Pixel.well(gridX(), py + 20, gridX() + 120, py + 36);
        search.drawTextBox();
        if (search.getText().isEmpty() && !search.isFocused()) fontRendererObj.drawString("SEARCH...", gridX() + 4, py + 24, 0xFF6D6079);
        int cx = gridX();
        for (String p : packs) {
            int w = fontRendererObj.getStringWidth(p) + 10;
            if (cx + w > px + pw - 6) break;
            Pixel.button(cx, py + 40, w, 16, p, p.equals(pack) ? Pixel.Style.YELLOW : Pixel.Style.GRAY, mouseX, mouseY);
            cx += w + 3;
        }

        // Grid (first card = None, last card for capes = open your folder)
        int cols = cols(), total = total(), viewH = viewH();
        int maxScroll = Math.max(0, ((total + cols - 1) / cols) * (CARD_H + GAP) - viewH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(gridX() * f, mc.displayHeight - (gridY() + viewH) * f, (px + pw - gridX()) * f, viewH * f);
        String equipped = mod.cape;
        for (int i = 0; i < total; i++) {
            int x = gridX() + (i % cols) * (CARD_W + GAP);
            int y = gridY() + (i / cols) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < gridY() || y > gridY() + viewH) continue;
            boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H && mouseY >= gridY();
            if (i == 0) {
                card(x, y, "None", Catalog.NONE.equals(equipped), hover);
                Pixel.toggle(x + CARD_W / 2 - 8, y + 14, 16, false, false);
            } else if (i <= shown.size()) {
                Catalog.Item item = shown.get(i - 1);
                card(x, y, item.name, item.id.equals(equipped), hover);
                icon(item, x + CARD_W / 2, y + 6);
            } else {
                card(x, y, "Your folder", false, hover);
                Pixel.button(x + CARD_W / 2 - 10, y + 14, 20, 20, "+", Pixel.Style.COPPER, -1, -1);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (shown.isEmpty()) drawCenteredString(fontRendererObj, "NO CAPES FOUND", gridX() + (px + pw - gridX()) / 2, gridY() + CARD_H + 20, Pixel.DIM);
        if (maxScroll > 0) {
            int bx = px + pw - 5, rows = (total + cols - 1) / cols;
            int bh = Math.max(12, viewH * viewH / (rows * (CARD_H + GAP)));
            int by = gridY() + (viewH - bh) * scroll / maxScroll;
            drawRect(bx, gridY(), bx + 2, gridY() + viewH, Pixel.PANEL_LO);
            drawRect(bx, by, bx + 2, by + bh, Pixel.GRAY_HI);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void card(int x, int y, String name, boolean equipped, boolean hover) {
        if (equipped) Pixel.box(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, Pixel.GREEN, Pixel.GREEN_HI, Pixel.GREEN_LO);
        Pixel.box(x, y, x + CARD_W, y + CARD_H, hover ? 0xFF241D2C : Pixel.PANEL_LO, 0xFF2C2436, 0);
        String n = fontRendererObj.trimStringToWidth(name.toUpperCase(), CARD_W - 4);
        drawCenteredString(fontRendererObj, n, x + CARD_W / 2, y + CARD_H - 21, Pixel.CREAM);
        if (equipped) {
            int tw = fontRendererObj.getStringWidth("EQUIPPED") + 4;
            Pixel.tag(x + (CARD_W - tw) / 2, y + CARD_H - 12, "EQUIPPED", Pixel.GREEN_LO);
        }
    }

    private void icon(Catalog.Item item, int cx, int y) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        if (item.isFile()) {
            ResourceLocation thumb = mod.capes.thumbnail(item.id);
            if (thumb == null) { // still loading
                Pixel.well(cx - 10, y, cx + 10, y + 32);
                return;
            }
            mc.getTextureManager().bindTexture(thumb);
            uv(cx - 10, y, 20, 32, 0, 0, 1, 1);
        } else {
            ResourceLocation tex = mod.capes.get(item.id);
            if (tex == null) return;
            mc.getTextureManager().bindTexture(tex);
            uv(cx - 10, y, 20, 32, 1 / 64f, 1 / 32f, 11 / 64f, 17 / 32f);
        }
    }

    private static void uv(int x, int y, int w, int h, float u0, float v0, float u1, float v1) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + h, 0).tex(u0, v1).endVertex();
        wr.pos(x + w, y + h, 0).tex(u1, v1).endVertex();
        wr.pos(x + w, y, 0).tex(u1, v0).endVertex();
        wr.pos(x, y, 0).tex(u0, v0).endVertex();
        t.draw();
    }

    /** Like GuiInventory.drawEntityOnScreen but with a free rotation and a neutral pose. */
    private void drawPlayer(int x, int y, int scale, float rotation, EntityPlayerSP ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 100f);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180f, 0, 0, 1);
        float body = ent.renderYawOffset, yawB = ent.rotationYaw, pitch = ent.rotationPitch;
        float head = ent.rotationYawHead, prevHead = ent.prevRotationYawHead;
        GlStateManager.rotate(135f, 0, 1, 0);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135f, 0, 1, 0);
        GlStateManager.rotate(-8f, 1, 0, 0);
        GlStateManager.rotate(rotation, 0, 1, 0);
        ent.renderYawOffset = 0;
        ent.rotationYaw = 0;
        ent.rotationPitch = 0;
        ent.rotationYawHead = 0;
        ent.prevRotationYawHead = 0;
        RenderManager rm = mc.getRenderManager();
        rm.setPlayerViewY(180f);
        rm.setRenderShadow(false);
        rm.renderEntityWithPosYaw(ent, 0, 0, 0, 0, 1f);
        rm.setRenderShadow(true);
        ent.renderYawOffset = body;
        ent.rotationYaw = yawB;
        ent.rotationPitch = pitch;
        ent.rotationYawHead = head;
        ent.prevRotationYawHead = prevHead;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        int vx = px + 8, vy = py + 22, vw = 134, vh = ph - 54;
        if (hit(mouseX, mouseY, vx, vy, vw, vh)) {
            dragging = true;
            lastMouseX = mouseX;
            return;
        }
        if (hit(mouseX, mouseY, vx, py + ph - 26, 64, 18)) { yaw = 0; return; }
        if (hit(mouseX, mouseY, vx + 70, py + ph - 26, 64, 18)) { yaw = 180; return; }
        if (hit(mouseX, mouseY, px + pw - 66, py + 19, 58, 18)) {
            mc.displayGuiScreen(new SettingsScreen(this, mod));
            return;
        }
        search.setFocused(hit(mouseX, mouseY, gridX(), py + 20, 120, 16));
        int cx = gridX();
        for (String p : packs) {
            int w = fontRendererObj.getStringWidth(p) + 10;
            if (cx + w > px + pw - 6) break;
            if (hit(mouseX, mouseY, cx, py + 40, w, 16)) {
                pack = p;
                filter();
                return;
            }
            cx += w + 3;
        }
        if (mouseY < gridY() || mouseY >= gridY() + viewH()) return;
        int cols = cols(), total = total();
        for (int i = 0; i < total; i++) {
            int x = gridX() + (i % cols) * (CARD_W + GAP);
            int y = gridY() + (i / cols) * (CARD_H + GAP) - scroll;
            if (!hit(mouseX, mouseY, x, y, CARD_W, CARD_H)) continue;
            if (i == 0) mod.equip(Catalog.NONE);
            else if (i <= shown.size()) mod.equip(shown.get(i - 1).id);
            else openCapesFolder();
            if (mc.thePlayer != null) mc.thePlayer.playSound("random.click", 0.4f, 1.2f);
            yaw = 180;
            return;
        }
    }

    private void openCapesFolder() {
        final File dir = Catalog.customCapeDir(configDir());
        Thread t = new Thread(() -> {
            try {
                Desktop.getDesktop().open(dir);
            } catch (Exception ignored) {
            }
        }, "Pegasus-Wardrobe");
        t.setDaemon(true);
        t.start();
    }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel();
        if (w != 0) scroll -= Integer.signum(w) * (CARD_H + GAP);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (search.isFocused() && search.textboxKeyTyped(typedChar, keyCode)) filter();
    }

    @Override
    public void updateScreen() {
        if (search != null) search.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
