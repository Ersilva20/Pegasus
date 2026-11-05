package dev.mylauncher.core.mixin;

import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.MLCore;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Inject(method = "renderDebugBoundingBox", at = @At("HEAD"), cancellable = true)
    private void ml$hitbox(Entity entity, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        if (Hooks.hitbox != null && Hooks.hitbox.render(entity, x, y, z, yaw, partialTicks)) ci.cancel();
    }

    /** Settings > Entity Render Distance (players are always drawn). */
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void ml$entityDistance(Entity e, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (MLCore.settings == null || MLCore.settings.entityDistance >= 128 || e instanceof EntityPlayer) return;
        double d = MLCore.settings.entityDistance;
        if (e.getDistanceSq(camX, camY, camZ) > d * d) cir.setReturnValue(false);
    }
}
