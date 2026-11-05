package dev.mylauncher.mods.glintcolorizer;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = "ml_glintcolorizer", name = "Glint Colorizer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class GlintColorizerMod extends Feature {
    @Setting(name = "Item Glint Color", color = true)
    public int itemColor = 0xFF8040CC;
    @Setting(name = "Armor Glint Color", color = true)
    public int armorColor = 0xFF603099;
    @Setting(name = "Chroma (rainbow)")
    public boolean chroma = false;

    public GlintColorizerMod() {
        super("glintcolorizer", "Glint Colorizer", "Customize the color of enchantment glints", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        if (!isEnabled()) return;
        apply(itemColor, armorColor);
    }

    private static void apply(int item, int armor) {
        Hooks.itemGlint = item;
        Hooks.armorGlint = new float[]{((armor >> 16) & 0xFF) / 255f, ((armor >> 8) & 0xFF) / 255f, (armor & 0xFF) / 255f};
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (chroma && event.phase == TickEvent.Phase.START) {
            int c = RenderUtil.chroma(0);
            apply(c, c);
        }
    }

    @Override
    protected void onDisable() {
        Hooks.itemGlint = -8372020;
        Hooks.armorGlint = null;
    }
}
