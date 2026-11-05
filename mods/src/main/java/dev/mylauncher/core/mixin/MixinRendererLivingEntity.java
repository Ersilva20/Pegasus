package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

/** The first four FloatBuffer.put calls in setBrightness are the red "hurt" overlay (1, 0, 0, 0.3). */
@Mixin(RendererLivingEntity.class)
public class MixinRendererLivingEntity {
    private static float ml$channel(float vanilla, int shift) {
        int c = Hooks.hitColor;
        if (c == 0) return vanilla;
        return ((c >> shift) & 0xFF) / 255.0f;
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0, remap = false))
    private FloatBuffer ml$red(FloatBuffer buf, float v) {
        return buf.put(ml$channel(v, 16));
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1, remap = false))
    private FloatBuffer ml$green(FloatBuffer buf, float v) {
        return buf.put(ml$channel(v, 8));
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2, remap = false))
    private FloatBuffer ml$blue(FloatBuffer buf, float v) {
        return buf.put(ml$channel(v, 0));
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3, remap = false))
    private FloatBuffer ml$alpha(FloatBuffer buf, float v) {
        return buf.put(ml$channel(v, 24));
    }
}
