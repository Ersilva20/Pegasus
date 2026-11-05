package dev.mylauncher.mods.tabeditor;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = "ml_tabeditor", name = "Tab List", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class TabEditorMod extends Feature implements Hooks.PingRenderer {
    @Setting(name = "Scale", min = 0.5, max = 1.5, step = 0.05)
    public float scale = 1f;
    @Setting(name = "Show Ping as Number")
    public boolean numericPing = true;
    @Setting(name = "Hide Header")
    public boolean hideHeader = false;
    @Setting(name = "Hide Footer")
    public boolean hideFooter = false;

    public TabEditorMod() {
        super("tabeditor", "Tab List", "Change how the player list (Tab) looks.", Category.HUD);
    }

    @Override
    protected void onEnable() {
        onSettingsChanged();
        Hooks.tabPing = this;
    }

    @Override
    public void onSettingsChanged() {
        if (isEnabled()) Hooks.tabScale = scale;
    }

    @Override
    protected void onDisable() {
        Hooks.tabScale = 1f;
        if (Hooks.tabPing == this) Hooks.tabPing = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.ingameGUI == null) return;
        if (hideHeader) mc.ingameGUI.getTabList().setHeader(null);
        if (hideFooter) mc.ingameGUI.getTabList().setFooter(null);
    }

    @Override
    public boolean render(int width, int x, int y, NetworkPlayerInfo info) {
        if (!numericPing) return false;
        int ms = info.getResponseTime();
        String c = ms < 80 ? "§a" : ms < 150 ? "§2" : ms < 250 ? "§e" : ms < 400 ? "§6" : "§c";
        String t = c + ms;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + width - 1, y + 1, 0);
        GlStateManager.scale(0.75f, 0.75f, 1);
        mc.fontRendererObj.drawStringWithShadow(t, -mc.fontRendererObj.getStringWidth(t), 0, 0xFFFFFFFF);
        GlStateManager.popMatrix();
        return true;
    }
}
