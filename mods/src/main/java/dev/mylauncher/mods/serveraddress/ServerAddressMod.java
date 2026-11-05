package dev.mylauncher.mods.serveraddress;

import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.SimpleTextHud;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = "ml_serveraddress", name = "Server Address", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ServerAddressMod extends SimpleTextHud {
    @Setting(name = "Show in Singleplayer")
    public boolean singleplayer = false;

    public ServerAddressMod() {
        super("serveraddress", "Server Address", "Shows the address of the server you are playing on.", 0.5f, 0.06f);
    }

    @Override
    protected String text() {
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null) return singleplayer ? "Singleplayer" : null;
        return mc.getCurrentServerData().serverIP;
    }

    @Override
    protected String sample() {
        return "mc.hypixel.net";
    }
}
