package dev.mylauncher.mods.packorganizer;

import dev.mylauncher.core.Category;
import dev.mylauncher.core.Feature;
import dev.mylauncher.core.MLCore;
import dev.mylauncher.core.Setting;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sort resource packs into folders inside .minecraft/resourcepacks. Packs inside folders (up to the chosen
 * depth) appear in the Resource Packs screen, and selected ones are restored when the game starts.
 */
@Mod(modid = "ml_packorganizer", name = "Pack Organizer", version = "1.0.0", clientSideOnly = true, dependencies = "required-after:mlcore")
public class PackOrganizerMod extends Feature {
    @Setting(name = "Folder Depth", min = 1, max = 4, step = 1)
    public int depth = 2;

    private final Map<String, ResourcePackRepository.Entry> cache = new HashMap<String, ResourcePackRepository.Entry>();
    private boolean restored;

    public PackOrganizerMod() {
        super("packorganizer", "Pack Organizer", "Allows you to organize your resource packs into folders", Category.UTILITY);
    }

    private static boolean isPack(File f) {
        return (f.isFile() && f.getName().toLowerCase().endsWith(".zip")) || (f.isDirectory() && new File(f, "pack.mcmeta").isFile());
    }

    /** All packs found inside sub-folders, keyed by file name (what Minecraft saves in options.txt). */
    private Map<String, ResourcePackRepository.Entry> folderPacks() {
        Map<String, ResourcePackRepository.Entry> out = new HashMap<String, ResourcePackRepository.Entry>();
        try {
            ResourcePackRepository repo = mc.getResourcePackRepository();
            Constructor<ResourcePackRepository.Entry> ctor =
                    ResourcePackRepository.Entry.class.getDeclaredConstructor(ResourcePackRepository.class, File.class);
            ctor.setAccessible(true);
            File[] top = repo.getDirResourcepacks().listFiles();
            if (top != null) {
                for (File f : top) if (f.isDirectory() && !isPack(f)) scan(repo, ctor, f, 1, out);
            }
        } catch (Exception e) {
            MLCore.LOG.error("Pack Organizer could not scan folders", e);
        }
        return out;
    }

    private void scan(ResourcePackRepository repo, Constructor<ResourcePackRepository.Entry> ctor, File dir, int level,
                      Map<String, ResourcePackRepository.Entry> out) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (isPack(f)) {
                String key = f.getAbsolutePath();
                ResourcePackRepository.Entry entry = cache.get(key);
                if (entry == null) {
                    entry = ctor.newInstance(repo, f);
                    try {
                        entry.updateResourcePack();
                    } catch (Exception e) {
                        continue;
                    }
                    cache.put(key, entry);
                }
                if (!out.containsKey(entry.getResourcePackName())) out.put(entry.getResourcePackName(), entry);
            } else if (f.isDirectory() && level < depth) {
                scan(repo, ctor, f, level + 1, out);
            }
        }
    }

    @Override
    protected void onEnable() {
        if (restored || mc.gameSettings == null) return;
        restored = true;
        ResourcePackRepository repo = mc.getResourcePackRepository();
        Map<String, ResourcePackRepository.Entry> folder = folderPacks();
        if (folder.isEmpty()) return;
        List<ResourcePackRepository.Entry> active = new ArrayList<ResourcePackRepository.Entry>(repo.getRepositoryEntries());
        List<String> activeNames = new ArrayList<String>();
        for (ResourcePackRepository.Entry e : active) activeNames.add(e.getResourcePackName());
        List<ResourcePackRepository.Entry> ordered = new ArrayList<ResourcePackRepository.Entry>();
        boolean changed = false;
        // options.txt lists packs top priority first; the repository wants lowest priority first.
        List<String> wanted = new ArrayList<String>(mc.gameSettings.resourcePacks);
        java.util.Collections.reverse(wanted);
        for (String name : wanted) {
            int i = activeNames.indexOf(name);
            if (i >= 0) ordered.add(active.get(i));
            else if (folder.containsKey(name)) {
                ordered.add(folder.get(name));
                changed = true;
            }
        }
        if (changed) {
            repo.setRepositories(ordered);
            mc.scheduleResourcesRefresh();
        }
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiScreenResourcePacks)) return;
        GuiScreenResourcePacks gui = (GuiScreenResourcePacks) event.gui;
        try {
            List<ResourcePackListEntry> available = ReflectionHelper.getPrivateValue(GuiScreenResourcePacks.class, gui,
                    "availableResourcePacks", "field_146966_g");
            List<ResourcePackListEntry> selected = ReflectionHelper.getPrivateValue(GuiScreenResourcePacks.class, gui,
                    "selectedResourcePacks", "field_146969_h");
            if (available == null || selected == null) return;
            List<String> present = new ArrayList<String>();
            for (ResourcePackListEntry e : available) if (e instanceof ResourcePackListEntryFound) present.add(((ResourcePackListEntryFound) e).func_148318_i().getResourcePackName());
            for (ResourcePackListEntry e : selected) if (e instanceof ResourcePackListEntryFound) present.add(((ResourcePackListEntryFound) e).func_148318_i().getResourcePackName());
            for (ResourcePackRepository.Entry entry : folderPacks().values()) {
                if (!present.contains(entry.getResourcePackName())) available.add(new ResourcePackListEntryFound(gui, entry));
            }
        } catch (Exception e) {
            MLCore.LOG.error("Pack Organizer could not add folder packs", e);
        }
    }
}
