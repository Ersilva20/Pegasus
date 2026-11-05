package dev.mylauncher.mods.autotexthotkey;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/** Five keys (set them in Controls) that each send a chat message or command. */
@Mod(modid = "ml_autotexthotkey", name = "Auto Text Hot Key", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class AutoTextHotkeyMod extends Feature {
    @Setting(name = "Message 1")
    public String message1 = "/l";
    @Setting(name = "Message 2")
    public String message2 = "gg";
    @Setting(name = "Message 3")
    public String message3 = "";
    @Setting(name = "Message 4")
    public String message4 = "";
    @Setting(name = "Message 5")
    public String message5 = "";
    @Setting(name = "Cooldown (ms)", min = 0, max = 5000, step = 100)
    public int cooldown = 1000;

    private final KeyBinding[] keys = new KeyBinding[5];
    private long last;

    public AutoTextHotkeyMod() {
        super("autotexthotkey", "Auto Text Hot Key", "Set keys that send a command or chat message when pressed.", Category.CHAT);
        for (int i = 0; i < keys.length; i++) keys[i] = MLCore.key("Auto Text " + (i + 1), Keyboard.KEY_NONE);
    }

    private String message(int i) {
        switch (i) {
            case 0: return message1;
            case 1: return message2;
            case 2: return message3;
            case 3: return message4;
            default: return message5;
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        for (int i = 0; i < keys.length; i++) {
            if (!keys[i].isPressed()) continue;
            String m = message(i).trim();
            if (m.isEmpty() || System.currentTimeMillis() - last < cooldown) continue;
            last = System.currentTimeMillis();
            if (net.minecraftforge.client.ClientCommandHandler.instance.executeCommand(mc.thePlayer, m) == 0) {
                mc.thePlayer.sendChatMessage(m);
            }
        }
    }
}
