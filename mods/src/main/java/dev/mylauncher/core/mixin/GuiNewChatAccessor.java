package dev.mylauncher.core.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Chat line list access for the Chat mod (stacking repeated messages). */
@Mixin(GuiNewChat.class)
public interface GuiNewChatAccessor {
    @Accessor("chatLines")
    List<ChatLine> ml$getChatLines();
}
