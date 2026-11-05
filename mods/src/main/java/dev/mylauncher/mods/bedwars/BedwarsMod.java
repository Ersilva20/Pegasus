package dev.mylauncher.mods.bedwars;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/** Resource counter plus kills / final kills / beds broken for the current Bed Wars game. */
@Mod(modid = "ml_bedwars", name = "Hypixel Bedwars", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class BedwarsMod extends HudFeature {
    private static final Item[] RESOURCES = {Items.iron_ingot, Items.gold_ingot, Items.diamond, Items.emerald};

    @Setting(name = "Resource Counter")
    public boolean resources = true;
    @Setting(name = "Game Stats (kills, finals, beds)")
    public boolean stats = true;
    @Setting(name = "Background")
    public boolean background = true;

    private int kills, finals, beds;

    public BedwarsMod() {
        super("bedwars", "Hypixel Bedwars", "Various enhancements to assist you when playing Hypixel Bedwars", Category.HYPIXEL, false, 0f, 0.4f);
    }

    @SubscribeEvent
    public void onJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        kills = finals = beds = 0;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null || mc.thePlayer == null) return;
        String text = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        String me = mc.thePlayer.getName();
        if (text.startsWith("Protect your bed and destroy the enemy beds")) {
            kills = finals = beds = 0;
            return;
        }
        if (text.startsWith("BED DESTRUCTION") && text.contains(" by " + me)) {
            beds++;
            return;
        }
        // "<victim> was killed by <you>." / "... by <you>. FINAL KILL!"
        if (!text.startsWith(me + " ") && (text.contains(" by " + me + ".") || text.contains(" by " + me + "!") || text.endsWith(" by " + me))) {
            if (text.contains("FINAL KILL")) finals++;
            else kills++;
        }
    }

    private int count(Item item) {
        if (!inGame()) return 0;
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) if (s != null && s.getItem() == item) n += s.stackSize;
        return n;
    }

    @Override
    public int width() {
        return 90;
    }

    @Override
    public int height() {
        return (resources ? 4 * 17 : 0) + (stats ? 32 : 0) + 2;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        if (background) Gui.drawRect(0, 0, width(), height(), 0x6F000000);
        int y = 2;
        if (resources) {
            for (Item r : RESOURCES) {
                RenderUtil.item(new ItemStack(r), 2, y, false);
                mc.fontRendererObj.drawString(String.valueOf(count(r)), 22, y + 4, 0xFFFFFFFF, true);
                y += 17;
            }
        }
        if (stats) {
            mc.fontRendererObj.drawString("Kills: §a" + kills, 3, y + 1, 0xFFFFFFFF, true);
            mc.fontRendererObj.drawString("Finals: §c" + finals, 3, y + 11, 0xFFFFFFFF, true);
            mc.fontRendererObj.drawString("Beds: §e" + beds, 3, y + 21, 0xFFFFFFFF, true);
        }
    }
}
