# Pegasus components

Everything Pegasus does, split by component.

## 1. Launcher (`launcher/`)

### Home
- **PLAY**: installs what is missing, then starts Minecraft 1.8.9 Forge, with a progress bar per stage.
- **Auto-join**: optional server address to connect to as soon as the game opens.
- Quick stats: mods enabled, memory, Java 8, JVM preset.
- Shortcuts: screenshots folder, mods folder.

### Install engine (`src/core`)
- Downloads and verifies (SHA-1) Java 8 (Temurin), Minecraft 1.8.9 (client, libraries, assets, natives)
  and Forge 11.15.1.2318, with parallel downloads.
- Shared libraries/assets between instances. Data in `%APPDATA%\.pegasus`.
- Optimized first-run `options.txt` (render distance, VBOs, no vsync...) only when none exists.
- JVM presets: **Optimized** (G1, low pauses) or **Vanilla**, plus extra JVM arguments.

### Mods page
- Lists every mod in the instance (from `mcmod.info`): name, description, version, size.
- Tabs **YOURS** / **PEGASUS**, search, enable/disable (renames to `.jar.disabled`), remove.
- Add Forge 1.8.9 `.jar` mods with a file picker or by **drag and drop**.
- Built-in Pegasus mods are synced on every launch: new ones arrive enabled, disabled ones stay disabled
  but are updated, Pegasus-Core can never be disabled, and old/renamed jars are migrated or removed.

### Account
- **Microsoft login** in a window inside the launcher (email, then password or 2FA), using
  authorization code + PKCE. A sign-in-with-code fallback is available.
- Tokens refreshed automatically before each launch, encrypted with the OS (Electron `safeStorage`).
- **Offline mode** with a custom username (3-16 letters, numbers or `_`).

### Settings
- Memory slider, JVM preset, extra JVM arguments.
- Game window size, start in fullscreen, hide or keep the launcher while playing.
- Azure Client ID for Microsoft login, custom Java 8 path, keep Pegasus mods updated.
- **Import from Minecraft**: copies your existing 1.8.9 setup into Pegasus:
  - `options.txt` and keybinds,
  - servers (merged from every 1.8 profile, no duplicates, icons kept),
  - resource packs made for 1.8 (`pack_format` 1; `.rar` packs are reported, not copied),
  - screenshots,
  - Everything replaced is backed up to `instances/<name>/import-backup/<date>` first.

### Console
- Live game log with colors, copy, clear, open logs folder, force close the game.

### CLI (`src/cli.js`)
- `install` and `launch` without the UI, for testing.

## 2. Pegasus-Core (`mods/src/main/java/dev/mylauncher/core`)

Required by every Pegasus mod.

- **Mod menu** (Right Shift, `/pegasus` or `/ml`): search, categories, enable/disable, per-mod settings.
  Three layouts, switched with the buttons at the top right (saved as Settings > Mod Menu Layout):
  - **Cards**: small cards with an item icon for each mod, name, category and an on/off toggle.
  - **Grid**: 3 columns of 3:4 tiles: icon + name + gear on top, a live preview of the mod in the middle
    (HUD mods are drawn for real), an ENABLED / DISABLED button at the bottom; click anywhere else for its settings.
  - **List**: wide rows without icons: name, category, description, gear and toggle.
- **HUD editor**: drag elements, scroll to resize, right-click for settings, snapping to the screen
  center and edges (hold Ctrl to disable).
- Feature system: `@Setting` fields become sliders, toggles, color pickers and dropdowns automatically,
  saved to `config/pegasus/<id>.json`.
- All keybinds under Options > Controls > **Pegasus Mods**.
- **Mouse button keybinds fix**: Toggle Perspective, Smooth Camera and every Pegasus keybind react
  immediately when bound to a mouse button (vanilla 1.8.9 waits for the next keyboard press).
- **Side button fix**: on Windows, LWJGL 2 reads side button 4 as side button 5 while Shift, Ctrl or another
  mouse button is held (e.g. sprinting + Toggle Perspective dropped your item). Pegasus reads the real button.
- Pixel-art GUI kit shared by all in-game screens.

### Settings tab (built in, always on)
| Section | Settings |
|---|---|
| General | Hit Delay Fix, Minimal View Bobbing, Show Achievements, Disable Hotbar Scrolling, Borderless Fullscreen, Mod Menu Layout |
| Movement | Toggle Sprint, Toggle Sneak, Sprint/Sneak status text (movable in the HUD editor) |
| Performance | Max Unfocused FPS, Max Main Menu FPS, Entity Render Distance, Tile Entity Render Distance |

### Cosmetics tab (built in, always on)
- Wardrobe (COSMETICS tab, **O** key or `/wardrobe`) with search and pack tabs.
- Capes are PNG files in `config/pegasus/cosmetics/capes` (standard cape layout, any 2:1 size, HD works).
  Each sub-folder becomes a tab, e.g. `capes/og` = **OG**. Cape images stay on your PC; none ship with Pegasus.
- Optional shared registry URL (e.g. a GitHub Gist) so friends using Pegasus see each other's capes.

## 3. Mods (`mods/src/main/java/dev/mylauncher/mods`, one jar each)

**Crosshair**: built-in styles (cross, dot, circle, square, X...) plus a **crosshair editor**: draw your own
crosshair on a 15x15 pixel grid (left click draws, right click erases, mirror X/Y, built-in styles as presets,
undo with Ctrl+Z, color, live 1x/2x preview). Open it by clicking Crosshair in the mod menu.

**Cape Physics**: capes move like cloth (Verlet simulation): strength, quality, gravity, sway, stiffness,
wind, body collision, other players; **P** toggles it in game.

| Category | Mods |
|---|---|
| HUD | FPS, CPS, Memory Usage, Ping, Coordinates, Compass, Clock, Server Address, Day Counter, Reach Display, Combo Counter, Momentum, View Angles, Playtime, Stopwatch, Resource Pack HUD, Server TPS, Potion Counter, Item Counter, Armor Status, Potion Effects, Keystrokes, Inventory Viewer, WAILA, Hotbar Key Labels, Saturation, Pickup Log, Scoreboard, Tab List, Titles, Action Bar, Boss Bar, Debug Screen, Hotbar Layout, Minimap |
| PvP | Fight Stats, Knockback Trainer, Teammate Radar, Cooldowns, Damage Shake, Low Health Tint, Hit Flash Color, Hitbox, Old Animations, UHC Highlight, Ping Markers, Kill Sound |
| Visual | Crosshair, Zoom, FOV Control, Freelook, Hold Perspective, Mob Scale, Block Outline, Lighting Tweaks (Fullbright), Screen Overlays, Particle Tweaks, Glint Color, Shiny Potions, Item Physics, Flat Items, Held Item Tweaks, Nametags, Motion Blur, Background Blur, Vibrance |
| World | Time Changer, Weather Changer, Fog Control, Chunk Borders, TNT Timer, Build Limit, Light Levels, Waypoints, Custom Skies, WorldEdit CUI |
| Inventory | Inventory Extras, Slot Lock, Drop Guard, Slot Swap, Inventory Search, Menu Animations, Scrollable Tooltips |
| Chat | Chat (stacking, timestamps, background, history), Chat Macros, Auto Reply |
| Hypixel | Quick Join, Hypixel Toolkit (Auto GG, Auto Tip, hide lobby spam), Bed Wars Helper, Stats Lookup |
| Utility | Streamer Mode, GUI Scale, Pack Folders, Sound Mixer, Screenshot Manager, Mumble Link, Now Playing (Spotify), Horse Stats |

### Replay Mod (third party)
The official [Replay Mod](https://www.replaymod.com) 1.8.9 (2.6.28, by CrushedPixel and johni0702, GPL-3.0) is installed
by the launcher on PLAY: downloaded from replaymod.com and checked with SHA-1 (it is not stored in this repo).
It shows up in the launcher's PEGASUS tab and can be disabled there. Record, watch and render replays from the
main menu (Replay Viewer).

### Default keys
Zoom **C** · Freelook **Left Alt** · Chunk Borders **F9** · Light Levels **F7** · Add Waypoint **B** ·
Quick Join **J** · Slot Lock **L** · Slot Swap **K** · Ping Marker **Middle Mouse** · Wardrobe **O** ·
Cape Physics **P** · Mod Menu **Right Shift**

### Commands
`/pegasus` or `/ml` (menu) · `/wardrobe` · `/waypoint add|remove|list|toggle|color` (`/wp`) ·
`/stats <player> [bedwars|duels|skywars]` (needs your Hypixel API key)

### Notes
- Cooldowns, Teammate Radar and Ping Markers work fully client-side: no server support needed.
- Not included on purpose: cheats (X-Ray, BHop...) and features that don't exist in 1.8.9. 3D skins are planned.

### Server rules
Most servers allow HUD/visual mods. Some forbid Freelook, Auto Reply, auto-GG style automation,
Hitbox/Reach displays or Hit Delay Fix. Check each server's allowed-mods list first.
