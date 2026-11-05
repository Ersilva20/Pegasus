package dev.mylauncher.mods.motionblur;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.PostShaders;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

/** Blends each frame with the previous one using Minecraft's built-in "phosphor" shader. */
@Mod(modid = "ml_motionblur", name = "Motion Blur", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MotionBlurMod extends Feature implements PostShaders.Uniforms {
    private static final ResourceLocation SHADER = new ResourceLocation("shaders/post/phosphor.json");

    @Setting(name = "Strength", min = 1, max = 10, step = 1)
    public int strength = 5;

    public MotionBlurMod() {
        super("motionblur", "Motion Blur", "Blur your surroundings when moving.", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        PostShaders.request(this, 10, SHADER, this);
    }

    @Override
    protected void onDisable() {
        PostShaders.release(this);
    }

    @Override
    public void apply(ShaderGroup group) {
        float v = 0.5f + strength * 0.045f;
        PostShaders.setUniform(group, "Phosphor", v, v, v);
    }
}
