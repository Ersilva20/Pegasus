'use strict';
const fs = require('fs');
const os = require('os');
const { paths, ensureDir } = require('./paths');

function defaultMemory() {
  // Half of RAM, between 2 and 4 GB: 1.8.9 with mods rarely benefits from more.
  const totalMb = Math.floor(os.totalmem() / 1048576);
  return Math.max(2048, Math.min(4096, Math.floor(totalMb / 2 / 512) * 512));
}

const DEFAULTS = {
  username: 'Player',
  memoryMb: defaultMemory(),
  width: 1280,
  height: 720,
  fullscreen: false,
  afterLaunch: 'hide', // hide | keep
  jvmPreset: 'optimized', // optimized | vanilla
  extraJvmArgs: '',
  javaPath: '', // empty = launcher-managed Java 8
  instance: 'default',
  syncBundledMods: true,
  server: '', // quick-join address
  accountType: 'offline', // offline | microsoft
  azureClientId: '', // your Azure app's client ID for Microsoft login
};

function load() {
  try {
    const data = JSON.parse(fs.readFileSync(paths.settings, 'utf8'));
    return Object.assign({}, DEFAULTS, data);
  } catch (e) {
    return Object.assign({}, DEFAULTS);
  }
}

function save(settings) {
  ensureDir(paths.root);
  const clean = {};
  for (const k of Object.keys(DEFAULTS)) clean[k] = settings[k] !== undefined ? settings[k] : DEFAULTS[k];
  fs.writeFileSync(paths.settings, JSON.stringify(clean, null, 2));
  return clean;
}

module.exports = { load, save, DEFAULTS };
