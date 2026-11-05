'use strict';
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const { paths, ensureDir, bundledModsDir } = require('./paths');
const { downloadFile } = require('./download');

const DISABLED = '.disabled';

function sameFile(a, b) {
  try {
    const sa = fs.statSync(a), sb = fs.statSync(b);
    return sa.size === sb.size && fs.readFileSync(a).equals(fs.readFileSync(b));
  } catch (e) {
    return false;
  }
}

const CORE_RE = /^(Pegasus|ML)-Core-/;
const BUILTIN_RE = /^(Pegasus|ML)-/;

/**
 * Third-party mods Pegasus ships by downloading them from their official site (not stored in this repo).
 * They show up in the PEGASUS tab and can be disabled like the built-in ones.
 */
const EXTERNAL = [
  {
    name: 'Replay Mod',
    file: 'replaymod-1.8.9-2.6.28.jar',
    match: /^replaymod-1\.8\.9-.*\.jar(\.disabled)?$/,
    urls: [
      'https://minio.replaymod.com/replaymod/replaymod-1.8.9-2.6.28.jar',
      'https://www.replaymod.com/download/download_new.php?version=1.8.9-2.6.28',
    ],
    sha1: '5ae360f64a3233f455513fb75e0fe2604b57005e',
    size: 17723584,
  },
];

function isExternal(file) {
  return EXTERNAL.some((e) => e.match.test(file));
}

/**
 * The bundled mods were renamed from ML-*.jar to Pegasus-*.jar. Two copies of the same mod would crash
 * Forge, so old copies are removed; a mod you had turned off stays off under its new name.
 */
function migrateLegacy(src, dest) {
  const bundled = new Set(fs.readdirSync(src).filter((n) => n.endsWith('.jar')));
  for (const f of fs.readdirSync(dest)) {
    const m = f.match(/^ML-(.+\.jar)(\.disabled)?$/);
    if (!m) continue;
    const renamed = 'Pegasus-' + m[1];
    if (!bundled.has(renamed)) continue;
    if (m[2] && !fs.existsSync(path.join(dest, renamed))) {
      fs.renameSync(path.join(dest, f), path.join(dest, renamed + DISABLED));
    } else {
      fs.unlinkSync(path.join(dest, f));
    }
  }
}

// Bundled jars that no longer exist: merged into the core (Cosmetics; Toggle Sneak is now in Settings) or renamed.
const MERGED = ['Cosmetics', 'ToggleSneakSprint'];
const RENAMED = {
  Physics: 'CapePhysics',
  // 2026-11-05: mods renamed to Pegasus' own names (config ids did not change).
  DirectionHUD: 'Compass', PackDisplay: 'ResourcePackHUD', TPSHud: 'ServerTPS', KeyStrokes: 'Keystrokes',
  InventoryHud: 'InventoryViewer', HotbarKeyOverlay: 'HotbarKeyLabels', PvPInfo: 'FightStats', ItemTracker: 'PickupLog',
  TeamView: 'TeammateRadar', FOVChanger: 'FOVControl', Snaplook: 'HoldPerspective', FogCustomizer: 'FogControl',
  HurtCam: 'DamageShake', DamageTint: 'LowHealthTint', HitColor: 'HitFlashColor', MobSize: 'MobScale',
  Lighting: 'LightingTweaks', Overlay: 'ScreenOverlays', ParticleChanger: 'ParticleTweaks', GlintColorizer: 'GlintColor',
  ShinyPots: 'ShinyPotions', '2DItems': 'FlatItems', ItemCustomizer: 'HeldItemTweaks', UHCOverlay: 'UHCHighlight',
  '1.7Visuals': 'OldAnimations', NickHider: 'StreamerMode', TabEditor: 'TabList', F3Display: 'DebugScreen',
  MenuBlur: 'BackgroundBlur', ColorSaturation: 'Vibrance', TNTCountdown: 'TNTTimer', HeightLimit: 'BuildLimit',
  LightOverlay: 'LightLevels', Markers: 'PingMarkers', Inventory: 'InventoryExtras', SlotLocking: 'SlotLock',
  ItemDropProtection: 'DropGuard', SlotBinding: 'SlotSwap', HotbarCustomizer: 'HotbarLayout',
  ContainerAnimations: 'MenuAnimations', PackOrganizer: 'PackFolders', AutoTextHotKey: 'ChatMacros',
  AutoTextActions: 'AutoReply', HypixelQuickplay: 'QuickJoin', HypixelMods: 'HypixelToolkit',
  HypixelBedwars: 'BedWarsHelper', HypixelStats: 'StatsLookup', KillSounds: 'KillSound', BetterSounds: 'SoundMixer',
  ScreenshotUploader: 'ScreenshotManager', MediaPlayer: 'NowPlaying',
};
const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const RETIRED_RE = new RegExp(
  '^(Pegasus|ML)-(' + [...MERGED, ...Object.keys(RENAMED)].map(escapeRe).join('|') + ')(-[\\d.]+)?\\.jar(\\.disabled)?$');

function retireOld(src, dest) {
  const bundled = fs.readdirSync(src).filter((n) => n.endsWith('.jar'));
  for (const f of fs.readdirSync(dest)) {
    const m = f.match(RETIRED_RE);
    if (!m) continue;
    const to = RENAMED[m[2]] && bundled.find((n) => n.startsWith('Pegasus-' + RENAMED[m[2]] + '-'));
    // A mod you had turned off stays off under its new name (the copy loop then updates its contents).
    if (m[4] && to && !fs.existsSync(path.join(dest, to)) && !fs.existsSync(path.join(dest, to + DISABLED))) {
      fs.renameSync(path.join(dest, f), path.join(dest, to + DISABLED));
    } else {
      fs.unlinkSync(path.join(dest, f));
    }
  }
}

/**
 * Copies the bundled Pegasus-*.jar mods into the instance. New mods arrive enabled; if you disabled one
 * (file renamed to .jar.disabled) it stays disabled but still gets updated.
 */
function syncBundled(instance) {
  const src = bundledModsDir();
  const dest = ensureDir(paths.mods(instance));
  if (!fs.existsSync(src)) return { added: 0, updated: 0 };
  migrateLegacy(src, dest);
  retireOld(src, dest);
  let added = 0, updated = 0;
  for (const name of fs.readdirSync(src)) {
    if (!name.endsWith('.jar')) continue;
    const from = path.join(src, name);
    const on = path.join(dest, name), off = on + DISABLED;
    if (fs.existsSync(off)) {
      if (!sameFile(from, off)) { fs.copyFileSync(from, off); updated++; }
    } else if (fs.existsSync(on)) {
      if (!sameFile(from, on)) { fs.copyFileSync(from, on); updated++; }
    } else {
      fs.copyFileSync(from, on);
      added++;
    }
  }
  // Pegasus Core is required by every Pegasus mod; never leave it disabled.
  for (const f of fs.readdirSync(dest)) {
    if (/^(Pegasus|ML)-Core-.*\.jar\.disabled$/.test(f)) fs.renameSync(path.join(dest, f), path.join(dest, f.slice(0, -DISABLED.length)));
  }
  return { added, updated };
}

/**
 * Downloads the EXTERNAL mods into the instance (cached in shared/mods so other instances reuse them).
 * A disabled copy stays disabled; older versions of the same mod are replaced. Never fails the launch.
 */
async function syncExternal(instance, log = () => {}) {
  const dest = ensureDir(paths.mods(instance));
  for (const ext of EXTERNAL) {
    try {
      const present = fs.readdirSync(dest).filter((f) => ext.match.test(f));
      const disabled = present.length > 0 && present.every((f) => f.endsWith(DISABLED));
      const target = path.join(dest, ext.file + (disabled ? DISABLED : ''));
      if (present.length === 1 && path.join(dest, present[0]) === target && fs.statSync(target).size === ext.size) continue;
      const cached = path.join(paths.shared, 'mods', ext.file);
      log('Downloading ' + ext.name);
      await downloadFile(ext.urls, cached, ext.sha1, ext.size);
      for (const f of present) fs.unlinkSync(path.join(dest, f));
      fs.copyFileSync(cached, target);
    } catch (e) {
      log(ext.name + ' could not be installed: ' + e.message);
    }
  }
}

/** Reads name/description from mcmod.info when present. */
function readInfo(file) {
  try {
    const zip = new AdmZip(file);
    const entry = zip.getEntry('mcmod.info');
    if (!entry) return {};
    let data = JSON.parse(entry.getData().toString('utf8').replace(/^﻿/, ''));
    if (!Array.isArray(data)) data = data.modList || data.modlist || [];
    const m = data[0] || {};
    return { name: m.name, description: m.description, version: m.version, modid: m.modid };
  } catch (e) {
    return {};
  }
}

function list(instance) {
  const dir = ensureDir(paths.mods(instance));
  const out = [];
  for (const file of fs.readdirSync(dir)) {
    const enabled = file.endsWith('.jar');
    if (!enabled && !file.endsWith('.jar' + DISABLED)) continue;
    const full = path.join(dir, file);
    const info = readInfo(full);
    const base = enabled ? file : file.slice(0, -DISABLED.length);
    out.push({
      file: base,
      enabled,
      required: CORE_RE.test(base),
      builtIn: BUILTIN_RE.test(base) || isExternal(base),
      name: info.name || base.replace(/\.jar$/, ''),
      description: info.description || '',
      version: info.version || '',
      sizeKb: Math.round(fs.statSync(full).size / 1024),
    });
  }
  out.sort((a, b) => (a.builtIn === b.builtIn ? a.name.localeCompare(b.name) : a.builtIn ? 1 : -1));
  return out;
}

function setEnabled(instance, file, enabled) {
  if (CORE_RE.test(file) && !enabled) throw new Error('Pegasus Core is required by the other Pegasus mods.');
  const dir = paths.mods(instance);
  const on = path.join(dir, file), off = on + DISABLED;
  if (enabled && fs.existsSync(off)) fs.renameSync(off, on);
  if (!enabled && fs.existsSync(on)) fs.renameSync(on, off);
}

function addMods(instance, files) {
  const dir = ensureDir(paths.mods(instance));
  let n = 0;
  for (const f of files) {
    if (!f.toLowerCase().endsWith('.jar')) continue;
    fs.copyFileSync(f, path.join(dir, path.basename(f)));
    n++;
  }
  return n;
}

function remove(instance, file) {
  if (CORE_RE.test(file)) throw new Error('Pegasus Core is required by the other Pegasus mods.');
  const dir = paths.mods(instance);
  for (const f of [path.join(dir, file), path.join(dir, file + DISABLED)]) if (fs.existsSync(f)) fs.unlinkSync(f);
}

module.exports = { syncBundled, syncExternal, list, setEnabled, addMods, remove, EXTERNAL };
