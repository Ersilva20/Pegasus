package dev.mylauncher.mods.chat;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

@Mod(modid = "ml_chat", name = "Chat", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ChatMod extends Feature {
    @Setting(name = "Background Opacity (%)", min = 0, max = 100, step = 5)
    public int backgroundOpacity = 50;
    @Setting(name = "Background Color", color = true)
    public int backgroundColor = 0xFF000000;
    @Setting(name = "Stack Repeated Messages", description = "Shows \"message (x3)\" instead of repeating lines")
    public boolean stack = true;
    @Setting(name = "Timestamps")
    public boolean timestamps = false;
    @Setting(name = "24h Timestamps")
    public boolean timestamps24 = true;
    @Setting(name = "Chat History Lines", min = 100, max = 1000, step = 50)
    public int history = 300;

    private String lastText;
    private int repeat = 1;

    public ChatMod() {
        super("chat", "Chat", "Customize your chat options to your liking.", Category.CHAT);
    }

    @Override
    protected void onEnable() {
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        if (!isEnabled()) return;
        int a = Math.round(backgroundOpacity / 100f * 127);
        Hooks.chatBackground = (a << 24) | (backgroundColor & 0xFFFFFF);
        Hooks.chatHistory = history;
    }

    @Override
    protected void onDisable() {
        Hooks.chatBackground = Integer.MIN_VALUE;
        Hooks.chatHistory = 100;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.isCanceled() || event.type == 2 || event.message == null) return;
        String plain = event.message.getUnformattedText();
        if (stack) {
            GuiNewChat chat = mc.ingameGUI.getChatGUI();
            if (plain.equals(lastText) && !((dev.mylauncher.core.mixin.GuiNewChatAccessor) chat).ml$getChatLines().isEmpty() && !plain.trim().isEmpty()) {
                repeat++;
                ChatLine top = ((dev.mylauncher.core.mixin.GuiNewChatAccessor) chat).ml$getChatLines().get(0);
                if (top.getChatComponent().getUnformattedText().contains(plain)) {
                    ((dev.mylauncher.core.mixin.GuiNewChatAccessor) chat).ml$getChatLines().remove(0);
                    chat.refreshChat();
                }
                IChatComponent copy = event.message.createCopy();
                copy.appendSibling(new ChatComponentText(" (x" + repeat + ")").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY)));
                event.message = copy;
            } else {
                repeat = 1;
            }
            lastText = plain;
        }
        if (timestamps) {
            String time = new SimpleDateFormat(timestamps24 ? "HH:mm" : "h:mm a").format(new Date());
            IChatComponent prefix = new ChatComponentText("[" + time + "] ").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));
            prefix.appendSibling(event.message);
            event.message = prefix;
        }
    }
}
