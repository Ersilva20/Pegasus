package dev.mylauncher.core.mixin;

import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Access to post-processing passes so mods can set shader uniforms (saturation, blur radius). */
@Mixin(ShaderGroup.class)
public interface ShaderGroupAccessor {
    @Accessor("listShaders")
    List<Shader> ml$getShaders();
}
