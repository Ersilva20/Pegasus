'use strict';
const fs = require('fs');
const path = require('path');
const os = require('os');
const zlib = require('zlib');
const { paths, ensureDir } = require('./paths');

/**
 * Imports your existing Minecraft 1.8.9 setup into a Pegasus instance:
 * game options + keybinds, servers, 1.8 resource packs and screenshots.
 * Everything it replaces is copied to <instance>/import-backup/<date> first.
 */

const HOME = os.homedir();
const APPDATA = process.env.APPDATA || path.join(HOME, 'AppData', 'Roaming');
const MINECRAFT = path.join(APPDATA, '.minecraft');
const PROFILES = path.join(HOME, 'Documents', 'Minecarft', 'Profiles');

// ---------------------------------------------------------------- NBT (uncompressed, servers.dat)

function readNbt(buf) {
  let p = 0;
  const u8 = () => buf.readUInt8(p++);
  const i16 = () => { const v = buf.readInt16BE(p); p += 2; return v; };
  const i32 = () => { const v = buf.readInt32BE(p); p += 4; return v; };
  const str = () => { const n = buf.readUInt16BE(p); p += 2; const s = buf.toString('utf8', p, p + n); p += n; return s; };
  function payload(t) {
    switch (t) {
      case 1: return { t, v: buf.readInt8(p++) };
      case 2: return { t, v: i16() };
      case 3: return { t, v: i32() };
      case 4: { const v = buf.readBigInt64BE(p); p += 8; return { t, v }; }
      case 5: { const v = buf.readFloatBE(p); p += 4; return { t, v }; }
      case 6: { const v = buf.readDoubleBE(p); p += 8; return { t, v }; }
      case 7: { const n = i32(); const v = buf.subarray(p, p + n); p += n; return { t, v }; }
      case 8: return { t, v: str() };
      case 9: { const et = u8(); const n = i32(); const v = []; for (let i = 0; i < n; i++) v.push(payload(et)); return { t, et, v }; }
      case 10: {
        const v = {};
        for (;;) { const ct = u8(); if (ct === 0) break; const name = str(); v[name] = payload(ct); }
        return { t, v };
      }
      case 11: { const n = i32(); const v = []; for (let i = 0; i < n; i++) v.push(i32()); return { t, v }; }
      default: throw new Error('Bad NBT tag ' + t);
    }
  }
  if (u8() !== 10) throw new Error('Not an NBT compound');
  str();
  return payload(10);
}

function writeNbt(root) {
  const parts = [];
  const b = (n) => { const x = Buffer.alloc(n); parts.push(x); return x; };
  const str = (s) => { const d = Buffer.from(s, 'utf8'); b(2).writeUInt16BE(d.length); parts.push(d); };
  function payload(tag) {
    switch (tag.t) {
      case 1: b(1).writeInt8(tag.v); break;
      case 2: b(2).writeInt16BE(tag.v); break;
      case 3: b(4).writeInt32BE(tag.v); break;
      case 4: b(8).writeBigInt64BE(tag.v); break;
      case 5: b(4).writeFloatBE(tag.v); break;
      case 6: b(8).writeDoubleBE(tag.v); break;
      case 7: b(4).writeInt32BE(tag.v.length); parts.push(Buffer.from(tag.v)); break;
      case 8: str(tag.v); break;
      case 9: b(1).writeUInt8(tag.v.length ? tag.et : 0); b(4).writeInt32BE(tag.v.length); tag.v.forEach(payload); break;
      case 10:
        for (const [k, c] of Object.entries(tag.v)) { b(1).writeUInt8(c.t); str(k); payload(c); }
        b(1).writeUInt8(0);
        break;
      case 11: b(4).writeInt32BE(tag.v.length); tag.v.forEach((x) => b(4).writeInt32BE(x)); break;
      default: throw new Error('Bad NBT tag ' + tag.t);
    }
  }
  b(1).writeUInt8(10);
  str('');
  payload(root);
  return Buffer.concat(parts);
}

function readServers(file) {
  try {
    const root = readNbt(fs.readFileSync(file));
    return root.v.servers ? root.v.servers.v : [];
  } catch (e) {
    return [];
  }
}

// ---------------------------------------------------------------- options.txt

function readOptions(file) {
  const out = new Map();
  if (!fs.existsSync(file)) return out;
  const text = fs.readFileSync(file, 'utf8');
  for (const line of text.split(/\r?\n/)) {
    // Keybind names can contain ':' ("Media: Play / Pause"), their value never does.
    const i = line.startsWith('key_') ? line.lastIndexOf(':') : line.indexOf(':');
    if (i > 0) out.set(line.slice(0, i), line.slice(i + 1));
  }
  return out;
}

// Names saved by other clients that vanilla and Pegasus don't use.
const SKIP = new Set(['lastLaunchedVersion']);

function mergeOptions(target, source) {
  for (const [k, v] of source) if (!SKIP.has(k)) target.set(k, v);
  return target;
}

// ---------------------------------------------------------------- resource packs

/**
 * Reads one small file out of a zip by looking only at its central directory, so a 1 GB texture pack isn't
 * loaded into memory just to read pack.mcmeta. Returns null when there is no matching entry.
 */
function zipEntryText(file, nameRe) {
  const fd = fs.openSync(file, 'r');
  try {
    const size = fs.fstatSync(fd).size;
    const tailLen = Math.min(size, 65557);
    const tail = Buffer.alloc(tailLen);
    fs.readSync(fd, tail, 0, tailLen, size - tailLen);
    const eocd = tail.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
    if (eocd < 0) return null;
    const cdSize = tail.readUInt32LE(eocd + 12), cdOffset = tail.readUInt32LE(eocd + 16);
    const cd = Buffer.alloc(cdSize);
    fs.readSync(fd, cd, 0, cdSize, cdOffset);
    for (let p = 0; p + 46 <= cd.length && cd.readUInt32LE(p) === 0x02014b50;) {
      const method = cd.readUInt16LE(p + 10), compSize = cd.readUInt32LE(p + 20);
      const nameLen = cd.readUInt16LE(p + 28), extraLen = cd.readUInt16LE(p + 30), commentLen = cd.readUInt16LE(p + 32);
      const local = cd.readUInt32LE(p + 42);
      const name = cd.toString('utf8', p + 46, p + 46 + nameLen);
      p += 46 + nameLen + extraLen + commentLen;
      if (!nameRe.test(name) || compSize > 1 << 20) continue;
      const head = Buffer.alloc(30);
      fs.readSync(fd, head, 0, 30, local);
      const data = Buffer.alloc(compSize);
      fs.readSync(fd, data, 0, compSize, local + 30 + head.readUInt16LE(26) + head.readUInt16LE(28));
      if (method === 0) return data.toString('utf8');
      if (method === 8) return zlib.inflateRawSync(data).toString('utf8');
      return null;
    }
    return null;
  } finally {
    fs.closeSync(fd);
  }
}

/** pack_format of a resource pack zip/folder (1 = Minecraft 1.8), or null if it isn't one. */
function packFormat(file) {
  try {
    let text;
    if (fs.statSync(file).isDirectory()) {
      const meta = path.join(file, 'pack.mcmeta');
      if (!fs.existsSync(meta)) return null;
      text = fs.readFileSync(meta, 'utf8');
    } else if (/\.zip$/i.test(file)) {
      text = zipEntryText(file, /^([^/]+\/)?pack\.mcmeta$/i);
      if (text === null) return null;
    } else {
      return null;
    }
    const m = text.replace(/^﻿/, '').match(/"pack_format"\s*:\s*(\d+)/);
    return m ? Number(m[1]) : null;
  } catch (e) {
    return null;
  }
}

function copyRecursive(from, to) {
  if (fs.statSync(from).isDirectory()) {
    ensureDir(to);
    for (const n of fs.readdirSync(from)) copyRecursive(path.join(from, n), path.join(to, n));
  } else {
    fs.copyFileSync(from, to);
  }
}

// ---------------------------------------------------------------- import

/** Folders to read: .minecraft first, then the 1.8.9 Forge profiles. */
function sources() {
  const list = [];
  if (fs.existsSync(MINECRAFT)) list.push({ name: '.minecraft', dir: MINECRAFT, main: true });
  if (fs.existsSync(PROFILES)) {
    for (const n of fs.readdirSync(PROFILES)) {
      const dir = path.join(PROFILES, n);
      if (fs.statSync(dir).isDirectory()) list.push({ name: n, dir, main: false });
    }
  }
  return list;
}

function run(instance) {
  const game = ensureDir(paths.instance(instance));
  const stamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');
  const backup = path.join(game, 'import-backup', stamp);
  const save = (file) => {
    if (!fs.existsSync(file)) return;
    ensureDir(backup);
    fs.copyFileSync(file, path.join(backup, path.basename(file)));
  };
  const report = { options: false, servers: 0, packs: [], skippedPacks: [], screenshots: 0, fullscreen: null };
  const src = sources();
  const main = src.find((s) => s.main);

  // Options + keybinds.
  const optionsFile = path.join(game, 'options.txt');
  let options = readOptions(optionsFile);
  if (main) {
    const imported = readOptions(path.join(main.dir, 'options.txt'));
    if (imported.size) {
      save(optionsFile);
      options = mergeOptions(options, imported);
      report.options = true;
      if (imported.has('fullscreen')) report.fullscreen = imported.get('fullscreen') === 'true';
    }
  }

  // Resource packs made for 1.8 (pack_format 1) from every folder; .rar files can't be loaded by Minecraft.
  const packsDir = ensureDir(path.join(game, 'resourcepacks'));
  const seen = new Set(fs.readdirSync(packsDir).map((n) => n.toLowerCase()));
  for (const s of src) {
    const dir = path.join(s.dir, 'resourcepacks');
    if (!fs.existsSync(dir)) continue;
    for (const n of fs.readdirSync(dir)) {
      if (seen.has(n.toLowerCase())) continue;
      const full = path.join(dir, n);
      if (/\.rar$/i.test(n)) { if (!report.skippedPacks.includes(n)) report.skippedPacks.push(n); continue; }
      if (packFormat(full) !== 1) continue; // not a pack, or made for a newer Minecraft
      copyRecursive(full, path.join(packsDir, n));
      seen.add(n.toLowerCase());
      report.packs.push(n);
    }
  }
  // Keep only enabled packs that now exist in the instance.
  if (options.has('resourcePacks')) {
    try {
      const on = JSON.parse(options.get('resourcePacks')).filter((n) => fs.existsSync(path.join(packsDir, n)));
      options.set('resourcePacks', JSON.stringify(on));
    } catch (e) { /* leave as is */ }
  }
  if (report.options) fs.writeFileSync(optionsFile, [...options].map(([k, v]) => k + ':' + v).join('\n') + '\n');

  // Servers: Pegasus's own list first, then every server not already in it.
  const serversFile = path.join(game, 'servers.dat');
  const merged = readServers(serversFile);
  const ips = new Set(merged.map((t) => String(t.v.ip && t.v.ip.v).toLowerCase()));
  for (const s of src) {
    if (!s.main && !hasOld(s.dir)) continue;
    for (const t of readServers(path.join(s.dir, 'servers.dat'))) {
      const ip = String(t.v.ip && t.v.ip.v).toLowerCase();
      if (!t.v.ip || ips.has(ip)) continue;
      ips.add(ip);
      merged.push(t);
      report.servers++;
    }
  }
  if (report.servers) {
    save(serversFile);
    fs.writeFileSync(serversFile, writeNbt({ t: 10, v: { servers: { t: 9, et: 10, v: merged } } }));
  }

  // Screenshots (never overwrites).
  if (main && fs.existsSync(path.join(main.dir, 'screenshots'))) {
    const to = ensureDir(path.join(game, 'screenshots'));
    for (const n of fs.readdirSync(path.join(main.dir, 'screenshots'))) {
      if (fs.existsSync(path.join(to, n))) continue;
      fs.copyFileSync(path.join(main.dir, 'screenshots', n), path.join(to, n));
      report.screenshots++;
    }
  }

  report.backup = fs.existsSync(backup) ? backup : null;
  return report;
}

/** True for a Minecraft 1.8.x profile folder (its servers work on 1.8.9; modern modpack servers don't). */
function hasOld(dir) {
  const opts = readOptions(path.join(dir, 'options.txt'));
  if (opts.has('version')) return false; // 1.9+ writes a data version here
  return opts.size > 0;
}

module.exports = { run, sources, readNbt, writeNbt, packFormat };
