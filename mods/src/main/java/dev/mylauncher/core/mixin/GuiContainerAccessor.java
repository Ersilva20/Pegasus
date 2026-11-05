package dev.mylauncher.core.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Container position and hovered slot for inventory mods (search, slot locking, binding). */
@Mixin(GuiContainer.class)
public interface GuiContainerAccessor {
    @Accessor("guiLeft")
    int ml$getGuiLeft();

    @Accessor("guiLeft")
    void ml$setGuiLeft(int value);

    @Accessor("guiTop")
    int ml$getGuiTop();

    @Accessor("xSize")
    int ml$getXSize();

    @Accessor("ySize")
    int ml$getYSize();

    @Accessor("theSlot")
    Slot ml$getHoveredSlot();
}
