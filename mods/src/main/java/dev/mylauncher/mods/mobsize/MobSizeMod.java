package dev.mylauncher.mods.mobsize;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "ml_mobsize", name = "Mob Scale", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class MobSizeMod extends Feature {
    @Setting(name = "Mob Scale", min = 0.1, max = 3, step = 0.05)
    public float mobs = 1.0f;
    @Setting(name = "Other Players Scale", min = 0.1, max = 3, step = 0.05)
    public float players = 1.0f;
    @Setting(name = "Your Player Scale", min = 0.1, max = 3, step = 0.05)
    public float self = 1.0f;

    private EntityLivingBase scaled;

    public MobSizeMod() {
        super("mobsize", "Mob Scale", "Draws mobs bigger or smaller.", Category.VISUAL);
    }

    private float scaleFor(EntityLivingBase e) {
        if (e == mc.thePlayer) return self;
        if (e instanceof EntityPlayer) return players;
        return mobs;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPre(RenderLivingEvent.Pre<?> event) {
        if (event.isCanceled()) return;
        float s = scaleFor(event.entity);
        if (s == 1.0f) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(event.x, event.y, event.z);
        GlStateManager.scale(s, s, s);
        GlStateManager.translate(-event.x, -event.y, -event.z);
        scaled = event.entity;
    }

    @SubscribeEvent
    public void onPost(RenderLivingEvent.Post<?> event) {
        if (scaled == event.entity) {
            GlStateManager.popMatrix();
            scaled = null;
        }
    }
}
