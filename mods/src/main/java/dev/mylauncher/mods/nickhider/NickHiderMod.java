package dev.mylauncher.mods.nickhider;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Hooks;
import dev.mylauncher.core.Setting;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

/** Replaces names everywhere text is drawn (chat, tab, nametags, scoreboard) and can hide skins. */
@Mod(modid = "ml_nickhider", name = "Streamer Mode", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class NickHiderMod extends Feature implements Hooks.TextFilter, Hooks.SkinOverride {
    @Setting(name = "Your Displayed Name")
    public String nick = "You";
    @Setting(name = "Hide Your Skin")
    public boolean hideOwnSkin = false;
    @Setting(name = "Hide Other Names")
    public boolean hideOthers = false;
    @Setting(name = "Other Players' Name")
    public String othersName = "Player";
    @Setting(name = "Hide Other Skins")
    public boolean hideOtherSkins = false;

    private Set<String> others = new HashSet<String>();
    private int ticks;

    public NickHiderMod() {
        super("nickhider", "Streamer Mode", "Hide your own name and skin, or other players', on your screen.", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        Hooks.textFilters.add(this);
        Hooks.skin = this;
    }

    @Override
    protected void onDisable() {
        Hooks.textFilters.remove(this);
        if (Hooks.skin == this) Hooks.skin = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !hideOthers || mc.getNetHandler() == null) return;
        if (++ticks % 40 != 0) return;
        Set<String> names = new HashSet<String>();
        String me = mc.getSession().getUsername();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            String n = info.getGameProfile().getName();
            if (n != null && n.length() >= 3 && !n.equals(me)) names.add(n);
        }
        others = names;
    }

    @Override
    public String filter(String text) {
        String me = mc.getSession().getUsername();
        if (me != null && !nick.isEmpty() && text.contains(me)) text = text.replace(me, nick);
        if (hideOthers) {
            for (String n : others) if (text.contains(n)) text = text.replace(n, othersName);
        }
        return text;
    }

    @Override
    public ResourceLocation skin(AbstractClientPlayer player) {
        boolean self = player.getUniqueID().equals(mc.getSession().getProfile().getId())
                || player.getName().equals(mc.getSession().getUsername());
        if ((self && hideOwnSkin) || (!self && hideOtherSkins)) return DefaultPlayerSkin.getDefaultSkin(player.getUniqueID());
        return null;
    }
}
