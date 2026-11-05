package dev.mylauncher.mods.menublur;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.PostShaders;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_menublur", name = "Background Blur", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MenuBlurMod extends Feature implements PostShaders.Uniforms {
    private static final ResourceLocation SHADER = new ResourceLocation("shaders/post/blur.json");

    @Setting(name = "Blur Radius", min = 2, max = 30, step = 1)
    public int radius = 12;
    @Setting(name = "Fade In (ms)", min = 0, max = 1000, step = 50)
    public int fade = 200;
    @Setting(name = "Blur Behind Chat")
    public boolean chat = false;

    private long openedAt;

    public MenuBlurMod() {
        super("menublur", "Background Blur", "Blurs the game behind open menus.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        boolean blur = event.gui != null && mc.theWorld != null && (chat || !(event.gui instanceof GuiChat));
        if (blur) {
            if (mc.currentScreen == null) openedAt = System.currentTimeMillis();
            PostShaders.request(this, 100, SHADER, this);
        } else {
            PostShaders.release(this);
        }
    }

    @Override
    protected void onDisable() {
        PostShaders.release(this);
    }

    @Override
    public void apply(ShaderGroup group) {
        float t = fade <= 0 ? 1f : Math.min(1f, (System.currentTimeMillis() - openedAt) / (float) fade);
        PostShaders.setUniform(group, "Radius", Math.max(1f, radius * t));
    }
}
