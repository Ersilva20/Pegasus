'use strict';
const path = require('path');
const os = require('os');
const fs = require('fs');

/** Everything lives in %APPDATA%\.pegasus (separate from .minecraft). */
const BASE = process.env.APPDATA || path.join(os.homedir(), '.config');
const ROOT = process.env.PEGASUS_HOME || path.join(BASE, '.pegasus');

// The launcher used to be called MyLauncher: move its data over once so nothing is downloaded again.
const LEGACY = path.join(BASE, '.mylauncher');
if (!process.env.PEGASUS_HOME && !fs.existsSync(ROOT) && fs.existsSync(LEGACY)) {
  try {
    fs.renameSync(LEGACY, ROOT);
  } catch (e) {
    // Old folder locked (e.g. game running): start fresh instead of failing.
  }
}

const paths = {
  root: ROOT,
  settings: path.join(ROOT, 'settings.json'),
  runtime: path.join(ROOT, 'runtime'),
  shared: path.join(ROOT, 'shared'),
  libraries: path.join(ROOT, 'shared', 'libraries'),
  assets: path.join(ROOT, 'shared', 'assets'),
  versions: path.join(ROOT, 'shared', 'versions'),
  cache: path.join(ROOT, 'cache'),
  logs: path.join(ROOT, 'logs'),
  instance: (name) => path.join(ROOT, 'instances', name),
  natives: (name) => path.join(ROOT, 'instances', name, 'natives'),
  mods: (name) => path.join(ROOT, 'instances', name, 'mods'),
};

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

/** Folder with the Pegasus-*.jar mods shipped with the launcher (dev: <repo>/build/mods, packaged: resources/bundled-mods). */
function bundledModsDir() {
  const packaged = process.resourcesPath ? path.join(process.resourcesPath, 'bundled-mods') : null;
  if (packaged && fs.existsSync(packaged)) return packaged;
  return path.resolve(__dirname, '..', '..', '..', 'build', 'mods');
}

module.exports = { paths, ensureDir, bundledModsDir };
