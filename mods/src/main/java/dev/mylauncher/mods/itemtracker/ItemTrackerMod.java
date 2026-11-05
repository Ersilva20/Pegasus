package dev.mylauncher.mods.itemtracker;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Setting;
import dev.mylauncher.core.hud.HudFeature;
import dev.mylauncher.core.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Shows "+3 Iron Ingot" / "-1 Arrow" when your inventory changes. */
@Mod(modid = "ml_itemtracker", name = "Item Tracker", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class ItemTrackerMod extends HudFeature {
    private static final class Entry {
        ItemStack stack;
        int delta;
        long time;
    }

    @Setting(name = "Show For (seconds)", min = 1, max = 10, step = 0.5)
    public double duration = 3;
    @Setting(name = "Max Lines", min = 1, max = 10, step = 1)
    public int maxLines = 5;
    @Setting(name = "Background")
    public boolean background = true;

    private final List<Entry> entries = new ArrayList<Entry>();
    private Map<String, Integer> last;
    private final Map<String, ItemStack> samples = new HashMap<String, ItemStack>();

    public ItemTrackerMod() {
        super("itemtracker", "Item Tracker", "Shows you when and how much items you pick up or drop.", Category.HUD, false, 1f, 0.6f);
    }

    private static String key(ItemStack s) {
        return net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem()) + ":" + s.getMetadata();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!inGame()) {
            last = null;
            return;
        }
        Map<String, Integer> now = new HashMap<String, Integer>();
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s == null || s.getItem() == null) continue;
            String k = key(s);
            Integer n = now.get(k);
            now.put(k, (n == null ? 0 : n) + s.stackSize);
            if (!samples.containsKey(k)) samples.put(k, s.copy());
        }
        // Ignore changes while a container is open (moving items around is not picking up).
        if (last != null && mc.currentScreen == null) {
            for (Map.Entry<String, Integer> e : now.entrySet()) {
                Integer before = last.get(e.getKey());
                int d = e.getValue() - (before == null ? 0 : before);
                if (d != 0) add(e.getKey(), d);
            }
            for (Map.Entry<String, Integer> e : last.entrySet()) {
                if (!now.containsKey(e.getKey())) add(e.getKey(), -e.getValue());
            }
        }
        last = now;
        long cutoff = System.currentTimeMillis() - (long) (duration * 1000);
        for (Iterator<Entry> it = entries.iterator(); it.hasNext(); ) if (it.next().time < cutoff) it.remove();
    }

    private void add(String key, int delta) {
        for (Entry e : entries) {
            if (key(e.stack).equals(key)) {
                e.delta += delta;
                e.time = System.currentTimeMillis();
                return;
            }
        }
        Entry e = new Entry();
        e.stack = samples.get(key);
        if (e.stack == null) return;
        e.delta = delta;
        e.time = System.currentTimeMillis();
        entries.add(0, e);
        while (entries.size() > maxLines) entries.remove(entries.size() - 1);
    }

    @Override
    public boolean shouldRender() {
        return !entries.isEmpty();
    }

    @Override
    public int width() {
        return 120;
    }

    @Override
    public int height() {
        return Math.max(1, Math.min(maxLines, Math.max(entries.size(), 1))) * 18;
    }

    @Override
    public void render(float partialTicks, boolean editing) {
        List<Entry> list = entries;
        if (list.isEmpty() && editing) {
            Entry e = new Entry();
            e.stack = new ItemStack(Items.iron_ingot);
            e.delta = 3;
            list = new ArrayList<Entry>();
            list.add(e);
        }
        int y = 0;
        for (Entry e : list) {
            if (background) Gui.drawRect(0, y, 120, y + 17, 0x6F000000);
            RenderUtil.item(e.stack, 1, y, false);
            String t = (e.delta > 0 ? "§a+" : "§c") + e.delta + " §f" + e.stack.getDisplayName();
            mc.fontRendererObj.drawString(mc.fontRendererObj.trimStringToWidth(t, 100), 19, y + 5, 0xFFFFFFFF, true);
            y += 18;
        }
    }
}
