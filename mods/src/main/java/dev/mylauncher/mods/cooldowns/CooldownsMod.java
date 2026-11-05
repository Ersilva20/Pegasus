package dev.mylauncher.mods.cooldowns;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cooldown timers. Lunar gets cooldowns from servers through its own API (Apollo);
 * here you list items and their cooldown, e.g. "ender_pearl:15, golden_apple:5", and a timer
 * starts when you use one.
 */
@Mod(modid = "ml_cooldowns", name = "Cooldowns", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class CooldownsMod extends HudFeature {
    @Setting(name = "Items (id:seconds, comma separated)")
    public String items = "ender_pearl:15, golden_apple:0";
    @Setting(name = "Background")
    public boolean background = true;

    private final Map<Item, Double> cooldowns = new LinkedHashMap<Item, Double>();
    private final Map<Item, Long> active = new LinkedHashMap<Item, Long>();

    public CooldownsMod() {
        super("cooldowns", "Cooldowns", "Displays items or abilities that are on cooldown on the HUD.", Category.PVP, false, 0.5f, 0.55f);
        onSettingsChanged();
    }

    @Override
    public void onSettingsChanged() {
        cooldowns.clear();
        for (String part : items.split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length < 2) continue;
            String id = kv.length == 3 ? kv[0] + ":" + kv[1] : kv[0];
            String secs = kv[kv.length - 1];
            Item item = Item.getByNameOrId(id.contains(":") ? id : "minecraft:" + id);
            try {
                double s = Double.parseDouble(secs.trim());
                if (item != null && s > 0) cooldowns.put(item, s);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onUse(PlayerInteractEvent event) {
        if (event.entityPlayer != mc.thePlayer || event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;
        Double cd = cooldowns.get(held.getItem());
        if (cd == null) return;
        Long until = active.get(held.getItem());
        if (until != null && until > System.currentTimeMillis()) return;
        active.put(held.getItem(), System.currentTimeMillis() + (long) (cd * 1000));
    }

    private List<Map.Entry<Item, Long>> current() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<Item, Long>> it = active.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() <= now) it.remove();
        }
        return new ArrayList<Map.Entry<Item, Long>>(active.entrySet());
    }

    @Override
    public boolean shouldRender() {
        return !current().isEmpty();
    }

    @Override
    public int width() {
        return Math.max(1, current().size()) * 40;
    }

    @Override
    public int height() {
        return 30;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        List<Map.Entry<Item, Long>> list = current();
        if (list.isEmpty() && editing) {
            Map<Item, Long> m = new LinkedHashMap<Item, Long>();
            m.put(Items.ender_pearl, System.currentTimeMillis() + 9300);
            list = new ArrayList<Map.Entry<Item, Long>>(m.entrySet());
        }
        int x = 0;
        for (Map.Entry<Item, Long> e : list) {
            if (background) Gui.drawRect(x, 0, x + 38, 30, 0x6F000000);
            RenderUtil.item(new ItemStack(e.getKey()), x + 11, 1, false);
            double left = Math.max(0, (e.getValue() - System.currentTimeMillis()) / 1000.0);
            String t = String.format("%.1fs", left);
            mc.fontRendererObj.drawString(t, x + 19 - mc.fontRendererObj.getStringWidth(t) / 2, 20, 0xFFFFFFFF, true);
            x += 40;
        }
    }
}
