package dev.mylauncher.mods.blockoutline;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

@Mod(modid = "ml_blockoutline", name = "Block Outline", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class BlockOutlineMod extends Feature {
    @Setting(name = "Outline Color", color = true)
    public int color = 0xFF000000;
    @Setting(name = "Line Width", min = 1, max = 10, step = 0.5)
    public float width = 2f;
    @Setting(name = "Fill Block")
    public boolean fill = false;
    @Setting(name = "Fill Color", color = true)
    public int fillColor = 0x30FFFFFF;
    @Setting(name = "See Through Blocks")
    public boolean throughBlocks = false;
    @Setting(name = "Chroma Outline")
    public boolean chroma = false;

    public BlockOutlineMod() {
        super("blockoutline", "Block Outline", "Change the color and width of the outline on the block you aim at, or fill it.", Category.VISUAL);
    }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent event) {
        MovingObjectPosition mop = event.target;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        event.setCanceled(true);
        BlockPos pos = mop.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.air || !mc.theWorld.getWorldBorder().contains(pos)) return;
        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        double px = mc.getRenderManager().viewerPosX, py = mc.getRenderManager().viewerPosY, pz = mc.getRenderManager().viewerPosZ;
        AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, pos).expand(0.002, 0.002, 0.002).offset(-px, -py, -pz);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (throughBlocks) GlStateManager.disableDepth();
        if (fill) RenderUtil.boxFilled(bb, fillColor);
        GL11.glLineWidth(width);
        RenderUtil.boxOutline(bb, chroma ? RenderUtil.chroma(0) : color);
        GL11.glLineWidth(1f);
        if (throughBlocks) GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }
}
