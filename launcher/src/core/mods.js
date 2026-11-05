'use strict';
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const { paths, ensureDir, bundledModsDir } = require('./paths');

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
const RETIRED_RE = /^(Pegasus|ML)-(Cosmetics|ToggleSneakSprint|Physics)(-[\d.]+)?\.jar(\.disabled)?$/;
const RENAMED = { Physics: 'CapePhysics' };

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
      builtIn: BUILTIN_RE.test(base),
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

module.exports = { syncBundled, list, setEnabled, addMods, remove };
