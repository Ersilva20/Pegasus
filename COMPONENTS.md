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
- **Import from Lunar / Minecraft**: copies your existing 1.8.9 setup into Pegasus:
  - `options.txt` and keybinds (from Lunar's `optionsLC.txt` when present),
  - servers (merged from every 1.8 profile, no duplicates, icons kept),
  - resource packs made for 1.8 (`pack_format` 1; `.rar` packs are reported, not copied),
  - screenshots,
  - the active Lunar profile's HUD layout: which mods are on, FPS/CPS/Keystrokes positions and text style.
  - Everything replaced is backed up to `instances/<name>/import-backup/<date>` first.

### Console
- Live game log with colors, copy, clear, open logs folder, force close the game.

### CLI (`src/cli.js`)
- `install` and `launch` without the UI, for testing.

## 2. Pegasus-Core (`mods/src/main/java/dev/mylauncher/core`)

Required by every Pegasus mod.

- **Mod menu** (Right Shift, `/pegasus` or `/ml`): search, categories, enable/disable, per-mod settings.
- **HUD editor**: drag elements, scroll to resize, right-click for settings, snapping to the screen
  center and edges (hold Ctrl to disable).
- Feature system: `@Setting` fields become sliders, toggles, color pickers and dropdowns automatically,
  saved to `config/pegasus/<id>.json`.
- All keybinds under Options > Controls > **Pegasus Mods**.
- Pixel-art GUI kit shared by all in-game screens.

### Settings tab (built in, always on)
| Section | Settings |
|---|---|
| General | Hit Delay Fix, Minimal View Bobbing, Show Achievements, Disable Hotbar Scrolling, Borderless Fullscreen |
| Movement | Toggle Sprint, Toggle Sneak, Sprint/Sneak status text (movable in the HUD editor) |
| Performance | Max Unfocused FPS, Max Main Menu FPS, Entity Render Distance, Tile Entity Render Distance |

### Cosmetics tab (built in, always on)
- Wardrobe (COSMETICS tab, **O** key or `/wardrobe`) with search and pack tabs.
- 15 built-in capes, most animated: Pegasus, Pegasus Day, Aurora, Galaxy, Synthwave, Ocean, Flame, Sakura,
  Ender Eye, Royal, Storm, Matrix, Frost, Rainbow, Checkers.
- Your own capes: PNGs in `config/pegasus/cosmetics/capes` (each sub-folder becomes a tab).
- Optional shared registry URL (e.g. a GitHub Gist) so friends using Pegasus see each other's capes.

## 3. Mods (`mods/src/main/java/dev/mylauncher/mods`, one jar each)

**Cape Physics**: capes move like cloth (Verlet simulation): strength, quality, gravity, sway, stiffness,
wind, body collision, other players; **P** toggles it in game.

| Category | Mods |
|---|---|
| HUD | FPS, CPS, Memory Usage, Ping, Coordinates, Direction HUD, Clock, Server Address, Day Counter, Reach Display, Combo Counter, Momentum, View Angles, Playtime, Stopwatch, Pack Display, TPS Hud, Potion Counter, Item Counter, Armor Status, Potion Effects, Key Strokes, Inventory Hud, WAILA, Hotbar Key Overlay, Saturation, Item Tracker, Scoreboard, Tab Editor, Titles, Action Bar, Boss Bar, F3 Display, Hotbar Customizer, Minimap |
| PvP | PvP Info, Knockback Trainer, Team View, Cooldowns, Hurt Cam, Damage Tint, Hit Color, Hitbox, 1.7 Visuals, UHC Overlay, Markers, Kill Sounds |
| Visual | Crosshair, Zoom, FOV Changer, Freelook, Snaplook, Mob Size, Block Outline, Lighting (Fullbright), Overlay, Particle Changer, Glint Colorizer, Shiny Pots, Item Physics, 2D Items, Item Customizer, Nametags, Motion Blur, Menu Blur, Color Saturation |
| World | Time Changer, Weather Changer, Fog Customizer, Chunk Borders, TNT Countdown, Height Limit, Light Overlay, Waypoints, Custom Skies, WorldEdit CUI |
| Inventory | Inventory, Slot Locking, Item Drop Protection, Slot Binding, Inventory Search, Container Animations, Scrollable Tooltips |
| Chat | Chat (stacking, timestamps, background, history), Auto Text Hot Key, Auto Text Actions |
| Hypixel | Hypixel Quickplay, Hypixel Mods (Auto GG, Auto Tip, hide lobby spam), Hypixel Bedwars, Hypixel Stats |
| Utility | Nick Hider, GUI Scale, Pack Organizer, Better Sounds, Screenshot Uploader, Mumble Link, Media Player (Spotify), Horse Stats |

### Default keys
Zoom **C** · Freelook **Left Alt** · Chunk Borders **F9** · Light Overlay **F7** · Add Waypoint **B** ·
Quickplay **J** · Slot Lock **L** · Slot Bind **K** · Marker **Middle Mouse** · Wardrobe **O** ·
Cape Physics **P** · Mod Menu **Right Shift**

### Commands
`/pegasus` or `/ml` (menu) · `/wardrobe` · `/waypoint add|remove|list|toggle|color` (`/wp`) ·
`/stats <player> [bedwars|duels|skywars]` (needs your Hypixel API key)

### Differences from Lunar
- Cooldowns, Team View and Markers work client-side (Lunar gets them from servers through its Apollo API).
- Screenshot Uploader opens/copies/shows the file instead of uploading to Lunar's service.
- Not included: Radio (licensed service), Replay/Rewind (use ReplayMod), Tier Tagger (no public API),
  3D Skins (planned), staff cheats (X-Ray/BHop), and features that don't exist in 1.8.9.

### Server rules
Most servers allow HUD/visual mods. Some forbid Freelook, Auto Text Actions, auto-GG style automation,
Hitbox/Reach displays or Hit Delay Fix. Check each server's allowed-mods list first.
