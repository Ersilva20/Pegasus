package dev.mylauncher.mods.hitcolor;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = "ml_hitcolor", name = "Hit Flash Color", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HitColorMod extends Feature {
    @Setting(name = "Color", description = "Alpha controls how strong the tint is", color = true)
    public int color = 0x4D00AAFF;
    @Setting(name = "Chroma (rainbow)")
    public boolean chroma = false;

    public HitColorMod() {
        super("hitcolor", "Hit Flash Color", "Pick the color players and mobs flash when they get hit.", Category.PVP);
    }

    @Override
    protected void onEnable() {
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        if (isEnabled()) Hooks.hitColor = color == 0 ? 1 : color;
    }

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent event) {
        if (chroma && event.phase == TickEvent.Phase.START) {
            Hooks.hitColor = (color & 0xFF000000) | (RenderUtil.chroma(0) & 0xFFFFFF);
        }
    }

    @Override
    protected void onDisable() {
        Hooks.hitColor = 0;
    }
}
