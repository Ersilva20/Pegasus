# Pegasus

A personal Minecraft **1.8.9** PvP client: an Electron launcher with a pixel-art UI plus a set of
Lunar-style Forge mods written from scratch. Unlike Lunar Client, Pegasus runs on plain **Forge**, so any
Forge 1.8.9 mod can be added next to the built-in ones.

See [COMPONENTS.md](COMPONENTS.md) for everything the launcher and the mods do.

## Repository layout

```
Pegasus/
├─ launcher/          Electron launcher (Windows)
│  ├─ src/main.js     main process: window, IPC, Microsoft login window, game process
│  ├─ src/preload.js  the only API the UI can call
│  ├─ src/core/       engine: Java 8, Minecraft, Forge, mods, auth, import (no Electron needed)
│  ├─ src/ui/         HTML/CSS/JS of the launcher (fonts and icons included)
│  └─ src/cli.js      command-line front end for testing without the UI
├─ mods/              Forge 1.8.9 mods (Gradle + essential-loom)
│  ├─ src/main/java/dev/mylauncher/core/   Pegasus-Core: features, settings, HUD, GUIs, mixins, cosmetics
│  ├─ src/main/java/dev/mylauncher/mods/   one package per mod
│  └─ mods.tsv        list of mods -> one jar each
├─ docs/              technical notes (Microsoft login setup)
└─ build/mods/        built jars (generated, not committed)
```

> Internal names `dev.mylauncher`, `mlcore` and `ml_*` are kept on purpose (the project was called
> MyLauncher at first) so existing configs keep working.

## Requirements

- Windows 10/11
- [Node.js](https://nodejs.org) 18+ (launcher)
- JDK 17 to run Gradle, and JDK 8 for Minecraft 1.8.9 (Gradle downloads it automatically if missing)

## Build

```bash
# 1. Build Pegasus-Core and every mod jar into build/mods
cd mods
./gradlew buildAllMods

# 2. Run the launcher in development
cd ../launcher
npm install
npm start

# 3. Package the launcher (output: launcher/dist/win-unpacked/Pegasus.exe, with the jars from build/mods)
npm run dist
```

The launcher installs everything else on first PLAY: Java 8 (Temurin), Minecraft 1.8.9 and Forge
11.15.1.2318. Game data lives in `%APPDATA%\.pegasus` (separate from `.minecraft` and `.lunarclient`).

### Testing

| Command | What it does |
|---|---|
| `cd mods && ./gradlew testClient` | Dev 1.8.9 client with Pegasus-Core and all mods (offline account) |
| `cd mods && ./gradlew testClient -Pdevtest=true` | Same, but walks through the menus and saves screenshots to `mods/run/screenshots` |
| `cd launcher && node src/cli.js install` | Downloads/verifies Java, Minecraft and Forge without the UI |
| `cd launcher && node src/cli.js launch` | Installs if needed and starts the game from the terminal |

## Adding a mod

1. Create a package `mods/src/main/java/dev/mylauncher/mods/<id>/` with a class extending `Feature`
   (or `HudFeature` / `SimpleTextHud` for HUD elements). Settings are public fields annotated with `@Setting`.
2. Add a line to `mods/mods.tsv`: `<id> <TAB> <JarName> <TAB> <Display name> <TAB> <description>`.
3. `./gradlew buildAllMods` produces `build/mods/Pegasus-<JarName>-1.0.0.jar`.

Feature settings are saved automatically to `config/pegasus/<id>.json` in the game folder.

## Microsoft login

Online servers need a Microsoft account. The launcher opens a Microsoft sign-in window (email, then
password or 2FA). It needs your own free Azure app ID once; see [docs/microsoft-login.md](docs/microsoft-login.md).
Offline mode works for singleplayer and offline servers.

## Notes

- Pegasus is a personal project, not affiliated with Mojang, Microsoft or Lunar Client.
- No Lunar Client code or assets are included. Personal cape images are only ever stored locally in the
  game folder and are never part of this repository.
