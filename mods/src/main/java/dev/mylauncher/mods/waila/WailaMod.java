package dev.mylauncher.mods.waila;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.Mod;

/** "What Am I Looking At": block or entity name, icon and extra info. */
@Mod(modid = "ml_waila", name = "WAILA", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class WailaMod extends HudFeature {
    @Setting(name = "Show Entities")
    public boolean entities = true;
    @Setting(name = "Show Block Position")
    public boolean position = false;
    @Setting(name = "Show Harvest Tool")
    public boolean tool = true;
    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0xC0100010;
    @Setting(name = "Border Color", color = true)
    public int borderColor = 0xFF5000FF;

    private String title;
    private String sub;
    private ItemStack icon;

    public WailaMod() {
        super("waila", "WAILA", "Names the block or mob under your crosshair.", Category.HUD, false, 0.5f, 0f);
    }

    private boolean update(boolean editing) {
        title = null;
        sub = null;
        icon = null;
        if (inGame()) {
            MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos pos = mop.getBlockPos();
                IBlockState state = mc.theWorld.getBlockState(pos);
                Block b = state.getBlock();
                if (b != Blocks.air) {
                    Item item = Item.getItemFromBlock(b);
                    ItemStack stack = item == null ? null : new ItemStack(item, 1, b.damageDropped(state));
                    try {
                        ItemStack picked = b.getPickBlock(mop, mc.theWorld, pos, mc.thePlayer);
                        if (picked != null) stack = picked;
                    } catch (Exception ignored) {
                    }
                    icon = stack;
                    title = stack != null && stack.getItem() != null ? stack.getDisplayName() : b.getLocalizedName();
                    StringBuilder s = new StringBuilder();
                    if (tool) {
                        String t = b.getHarvestTool(state);
                        if (t != null) s.append("Tool: ").append(t);
                    }
                    if (position) {
                        if (s.length() > 0) s.append("  ");
                        s.append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ());
                    }
                    sub = s.length() > 0 ? s.toString() : "Minecraft";
                }
            } else if (entities && mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                Entity e = mop.entityHit;
                title = e.getDisplayName().getFormattedText();
                if (e instanceof EntityLivingBase) {
                    EntityLivingBase l = (EntityLivingBase) e;
                    sub = String.format("❤ %.1f / %.1f", l.getHealth(), l.getMaxHealth());
                } else {
                    sub = "Entity";
                }
            }
        }
        if (title == null && editing) {
            title = "Grass Block";
            sub = "Minecraft";
            icon = new ItemStack(Blocks.grass);
        }
        return title != null;
    }

    @Override
    public boolean shouldRender() {
        return update(false);
    }

    @Override
    public int width() {
        update(true);
        int w = Math.max(mc.fontRendererObj.getStringWidth(title), mc.fontRendererObj.getStringWidth(sub == null ? "" : sub));
        return w + (icon != null ? 28 : 10);
    }

    @Override
    public int height() {
        return 28;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        update(editing);
        if (title == null) return;
        int w = width();
        Gui.drawRect(1, 0, w - 1, 28, backgroundColor);
        Gui.drawRect(0, 1, 1, 27, backgroundColor);
        Gui.drawRect(w - 1, 1, w, 27, backgroundColor);
        RenderUtil.outline(1, 1, w - 1, 27, borderColor);
        int tx = 6;
        if (icon != null) {
            RenderUtil.item(icon, 5, 6, false);
            tx = 24;
        }
        mc.fontRendererObj.drawString(title, tx, 5, 0xFFFFFFFF, true);
        if (sub != null) mc.fontRendererObj.drawString(sub, tx, 15, 0xFF5555FF, true);
    }
}
