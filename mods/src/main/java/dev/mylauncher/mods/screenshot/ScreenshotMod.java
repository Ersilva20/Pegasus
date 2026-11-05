package dev.mylauncher.mods.screenshot;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;

/**
 * Adds [Open] [Copy] [Folder] buttons to the "Saved screenshot" chat message, and can copy every new
 * screenshot to the clipboard automatically.
 */
@Mod(modid = "ml_screenshot", name = "Screenshot Manager", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ScreenshotMod extends Feature {
    @Setting(name = "Show Action Buttons")
    public boolean buttons = true;
    @Setting(name = "Auto Copy to Clipboard")
    public boolean autoCopy = false;

    private boolean registered;

    public ScreenshotMod() {
        super("screenshot", "Screenshot Manager", "After a screenshot: open it, copy it or show it in its folder.", Category.UTILITY);
    }

    private File dir() {
        return new File(mc.mcDataDir, "screenshots");
    }

    @Override
    protected void onEnable() {
        if (registered) return;
        registered = true;
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return "mlscreenshot";
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/mlscreenshot <open|copy|folder> <file>";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                if (args.length < 1) return;
                File f = args.length > 1 ? new File(dir(), new File(args[1]).getName()) : null;
                if (args[0].equals("folder")) open(dir());
                else if (args[0].equals("open") && f != null) open(f);
                else if (args[0].equals("copy") && f != null) copy(f);
            }

            @Override
            public int getRequiredPermissionLevel() {
                return 0;
            }

            @Override
            public boolean canCommandSenderUseCommand(ICommandSender sender) {
                return true;
            }
        });
    }

    private static void open(final File f) {
        Thread t = new Thread(() -> {
            try {
                Desktop.getDesktop().open(f);
            } catch (Exception e) {
                MLCore.LOG.warn("Could not open " + f, e);
            }
        }, "Pegasus-Screenshot");
        t.setDaemon(true);
        t.start();
    }

    private void copy(final File f) {
        Thread t = new Thread(() -> {
            try {
                final Image img = ImageIO.read(f);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.imageFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.imageFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) {
                        return img;
                    }
                }, null);
                mc.addScheduledTask(() -> MLCore.chat("Screenshot copied to clipboard."));
            } catch (Exception e) {
                MLCore.LOG.warn("Could not copy " + f, e);
            }
        }, "Pegasus-Screenshot");
        t.setDaemon(true);
        t.start();
    }

    private static IChatComponent button(String label, String command, String hover, EnumChatFormatting color) {
        IChatComponent c = new ChatComponentText(" [" + label + "]");
        c.setChatStyle(new ChatStyle().setColor(color).setBold(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(hover))));
        return c;
    }

    private static String findFileName(IChatComponent msg) {
        for (IChatComponent part : msg) {
            ClickEvent ce = part.getChatStyle().getChatClickEvent();
            if (ce != null && ce.getAction() == ClickEvent.Action.OPEN_FILE) return new File(ce.getValue()).getName();
        }
        return null;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) return;
        String text = event.message.getUnformattedText();
        if (!text.startsWith("Saved screenshot as")) return;
        String name = findFileName(event.message);
        if (name == null) return;
        if (autoCopy) copy(new File(dir(), name));
        if (!buttons) return;
        IChatComponent msg = event.message.createCopy();
        msg.appendSibling(button("Open", "/mlscreenshot open " + name, "Open the screenshot", EnumChatFormatting.GREEN));
        msg.appendSibling(button("Copy", "/mlscreenshot copy " + name, "Copy image to clipboard", EnumChatFormatting.AQUA));
        msg.appendSibling(button("Folder", "/mlscreenshot folder", "Open screenshots folder", EnumChatFormatting.YELLOW));
        event.message = msg;
    }
}
