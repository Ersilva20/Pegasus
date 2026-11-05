package dev.mylauncher.mods.killsounds;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Plays a sound when you kill a player (detected from your hits and from kill messages in chat). */
@Mod(modid = "ml_killsounds", name = "Kill Sounds", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class KillSoundsMod extends Feature {
    public enum Sound {
        ORB("random.orb"), LEVEL_UP("random.levelup"), ANVIL("random.anvil_land"), PLING("note.pling"),
        GHAST("mob.ghast.scream"), CAT("mob.cat.meow"), EXPLOSION("random.explode"), BLAZE("mob.blaze.death"),
        CUSTOM("");
        final String id;

        Sound(String id) {
            this.id = id;
        }
    }

    @Setting(name = "Sound")
    public Sound sound = Sound.ORB;
    @Setting(name = "Custom Sound Id (when Sound = Custom)")
    public String custom = "random.successful_hit";
    @Setting(name = "Volume", min = 0.1, max = 2, step = 0.1)
    public float volume = 1f;
    @Setting(name = "Pitch", min = 0.5, max = 2, step = 0.05)
    public float pitch = 1f;

    private EntityPlayer target;
    private long targetTime;
    private long lastPlayed;

    public KillSoundsMod() {
        super("killsounds", "Kill Sounds", "Allows you to change kill sounds for players.", Category.PVP);
    }

    private void play() {
        if (System.currentTimeMillis() - lastPlayed < 750 || mc.thePlayer == null) return;
        lastPlayed = System.currentTimeMillis();
        String id = sound == Sound.CUSTOM ? custom : sound.id;
        mc.thePlayer.playSound(id, volume, pitch);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer == mc.thePlayer && event.target instanceof EntityPlayer) {
            target = (EntityPlayer) event.target;
            targetTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || target == null) return;
        if (System.currentTimeMillis() - targetTime > 3000) {
            target = null;
            return;
        }
        if (target.getHealth() <= 0 || target.isDead) {
            play();
            target = null;
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null || mc.thePlayer == null || event.type == 2) return;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        String me = mc.thePlayer.getName();
        if (text.startsWith(me + " ") || text.contains(me + ":")) return;
        if (text.contains(" by " + me) || text.contains("killed by " + me) || text.contains("slain by " + me)) play();
    }
}
