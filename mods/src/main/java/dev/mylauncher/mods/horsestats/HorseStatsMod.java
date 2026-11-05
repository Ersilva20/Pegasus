package dev.mylauncher.mods.horsestats;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Speed, jump height and health of the horse whose inventory is open. */
@Mod(modid = "ml_horsestats", name = "Horse Stats", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class HorseStatsMod extends Feature {
    @Setting(name = "Decimals", min = 0, max = 3, step = 1)
    public int decimals = 2;

    public HorseStatsMod() {
        super("horsestats", "Horse Stats", "Shows the speed, jump height and health of horses.", Category.UTILITY);
    }

    /** Converts jump strength to blocks (fitted formula used by the community). */
    static double jumpHeight(double strength) {
        return -0.1817584952 * Math.pow(strength, 3) + 3.689713992 * Math.pow(strength, 2) + 2.128599134 * strength - 0.343930367;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiScreenHorseInventory)) return;
        EntityHorse horse;
        try {
            horse = ReflectionHelper.getPrivateValue(GuiScreenHorseInventory.class, (GuiScreenHorseInventory) event.gui, "horseEntity", "field_147034_x");
        } catch (Exception e) {
            return;
        }
        if (horse == null) return;
        String f = "%." + decimals + "f";
        double speed = horse.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue() * 43.17;
        double jump = jumpHeight(horse.getHorseJumpStrength());
        String[] lines = {
                "Speed: " + String.format(f, speed) + " m/s",
                "Jump: " + String.format(f, jump) + " blocks",
                "Health: " + String.format(f, horse.getMaxHealth() / 2) + " ❤"
        };
        int x = event.gui.width / 2 + 92, y = event.gui.height / 2 - 80;
        Gui.drawRect(x - 3, y - 3, x + 110, y + lines.length * 11, 0xC0000000);
        for (String l : lines) {
            mc.fontRendererObj.drawStringWithShadow(l, x, y, 0xFFFFFFFF);
            y += 11;
        }
    }
}
