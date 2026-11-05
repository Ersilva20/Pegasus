package dev.mylauncher.mods.colorsaturation;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.PostShaders;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_colorsaturation", name = "Vibrance", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ColorSaturationMod extends Feature implements PostShaders.Uniforms {
    private static final ResourceLocation SHADER = new ResourceLocation("shaders/post/color_convolve.json");

    @Setting(name = "Saturation", description = "0 = grayscale, 1 = normal, 2 = very vivid", min = 0, max = 2, step = 0.05)
    public float saturation = 1.4f;

    public ColorSaturationMod() {
        super("colorsaturation", "Vibrance", "Make the game's colors more vivid or more muted.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        PostShaders.request(this, 5, SHADER, this);
    }

    @Override
    protected void onDisable() {
        PostShaders.release(this);
    }

    @Override
    public void apply(ShaderGroup group) {
        PostShaders.setUniform(group, "Saturation", saturation);
    }
}
