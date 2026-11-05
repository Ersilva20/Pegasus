package dev.mylauncher.core;

import dev.mylauncher.core.gui.ModMenuScreen;
import dev.mylauncher.core.hud.HudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Mod(modid = MLCore.MODID, name = "Pegasus Core", version = "1.0.0", clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]")
public class MLCore {
    public static final String MODID = "mlcore";
    public static final String KEY_CATEGORY = "Pegasus Mods";
    public static final Logger LOG = LogManager.getLogger("Pegasus");

    private static final List<Feature> FEATURES = new ArrayList<Feature>();
    private static GuiScreen pendingScreen;
    public static KeyBinding menuKey;
    public static dev.mylauncher.core.cosmetics.CosmeticsMod cosmetics;
    public static ClientSettings settings;

    public static List<Feature> features() {
        return Collections.unmodifiableList(FEATURES);
    }

    /** Registers a key in Options > Controls under the "Pegasus Mods" category. */
    public static KeyBinding key(String name, int defaultKey) {
        KeyBinding k = new KeyBinding(name, defaultKey, KEY_CATEGORY);
        ClientRegistry.registerKeyBinding(k);
        return k;
    }

    public static void register(Feature f) {
        for (Feature existing : FEATURES) if (existing.id.equals(f.id)) return;
        FEATURES.add(f);
        Collections.sort(FEATURES, new Comparator<Feature>() {
            @Override
            public int compare(Feature a, Feature b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        f.captureDefaults();
        Config.load(f);
        if (f.builtIn()) f.setEnabled(true);
    }

    /** Opens a screen on the next client tick (needed when opening from a chat command). */
    public static void openNextTick(GuiScreen screen) {
        pendingScreen = screen;
    }

    public static void chat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[Pegasus] " + EnumChatFormatting.RESET + message));
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        menuKey = key("Open Mod Menu", Keyboard.KEY_RSHIFT);
        settings = new ClientSettings();
        register(settings);
        register(new SprintStatusHud(settings));
        cosmetics = new dev.mylauncher.core.cosmetics.CosmeticsMod();
        register(cosmetics);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
        MinecraftForge.EVENT_BUS.register(new dev.mylauncher.core.util.Packets());
        MinecraftForge.EVENT_BUS.register(new dev.mylauncher.core.util.PostShaders());
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return "ml";
            }

            @Override
            public java.util.List<String> getCommandAliases() {
                return java.util.Collections.singletonList("pegasus");
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/pegasus (or /ml) - open the Pegasus mod menu";
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                openNextTick(new ModMenuScreen());
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

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        int count = 0;
        for (ModContainer container : Loader.instance().getActiveModList()) {
            if (!container.getModId().startsWith("ml_")) continue;
            Object mod = container.getMod();
            if (mod instanceof FeatureProvider) {
                for (Feature f : ((FeatureProvider) mod).features()) {
                    register(f);
                    count++;
                }
            } else if (mod instanceof Feature) {
                register((Feature) mod);
                count++;
            }
        }
        LOG.info("Pegasus Core loaded {} features", count);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (menuKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().displayGuiScreen(new ModMenuScreen());
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && pendingScreen != null) {
            Minecraft.getMinecraft().displayGuiScreen(pendingScreen);
            pendingScreen = null;
        }
    }
}
