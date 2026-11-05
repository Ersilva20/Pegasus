package dev.mylauncher.core.cosmetics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The capes: PNG files in config/pegasus/cosmetics/capes. Each sub-folder is a pack (e.g. capes/og = the OG capes). */
public final class Catalog {
    public enum Type { CAPE }

    public static final class Item {
        public final String id;
        public final String name;
        public final Type type;
        /** The folder name for cape packs (upper case), "YOURS" for loose PNGs. */
        public final String pack;

        Item(String id, String name, Type type, String pack) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.pack = pack;
        }

        public boolean isFile() {
            return id.startsWith("file:");
        }
    }

    public static final String NONE = "none";
    public static final String YOURS = "YOURS";


    private Catalog() {
    }

    /**
     * Your own capes: PNG files in .minecraft/config/pegasus/cosmetics/capes (standard 64x32 cape layout or any
     * 2:1 size). Each sub-folder is a "pack" with its own tab in the Wardrobe (e.g. capes/friends).
     */
    public static File customCapeDir(File configDir) {
        File d = new File(configDir, "cosmetics/capes");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    static String prettyName(String file) {
        String base = file.substring(0, file.length() - 4).replace('_', ' ').replace('-', ' ').trim();
        StringBuilder sb = new StringBuilder();
        for (String w : base.split(" +")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static void addPngs(File dir, String prefix, String pack, List<Item> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".png")) {
                out.add(new Item("file:" + prefix + f.getName(), prettyName(f.getName()), Type.CAPE, pack));
            }
        }
    }

    public static List<Item> capesWithCustom(File configDir) {
        List<Item> out = new ArrayList<Item>();
        File root = customCapeDir(configDir);
        addPngs(root, "", YOURS, out);
        File[] dirs = root.listFiles();
        if (dirs != null) {
            Arrays.sort(dirs);
            for (File d : dirs) {
                if (d.isDirectory() && !d.getName().startsWith(".")) addPngs(d, d.getName() + "/", d.getName().toUpperCase(), out);
            }
        }
        return out;
    }

    /** Resolves a "file:" cape id to a file, refusing anything outside the capes folder. */
    static File capeFile(File configDir, String id) {
        if (id == null || !id.startsWith("file:")) return null;
        try {
            File root = customCapeDir(configDir).getCanonicalFile();
            File f = new File(root, id.substring(5)).getCanonicalFile();
            return f.getPath().startsWith(root.getPath() + File.separator) && f.isFile() ? f : null;
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
