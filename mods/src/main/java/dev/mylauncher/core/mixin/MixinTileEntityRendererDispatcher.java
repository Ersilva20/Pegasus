package dev.mylauncher.core.mixin;

import dev.mylauncher.core.MLCore;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Settings > Tile Entity Render Distance. */
@Mixin(TileEntityRendererDispatcher.class)
public class MixinTileEntityRendererDispatcher {
    @Shadow public double entityX;
    @Shadow public double entityY;
    @Shadow public double entityZ;

    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void ml$distance(TileEntity te, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (MLCore.settings == null || MLCore.settings.tileEntityDistance >= 64) return;
        double d = MLCore.settings.tileEntityDistance;
        if (te.getDistanceSq(entityX, entityY, entityZ) > d * d) ci.cancel();
    }
}
