'use strict';
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const { paths, ensureDir } = require('./paths');
const { installVanilla, extractNatives } = require('./minecraft');
const { installForge } = require('./forge');
const { ensureJava8 } = require('./java');
const mods = require('./mods');
const { offlineAccount } = require('./auth');

const MC_VERSION = '1.8.9';

/** Java 8 G1 tuning used by most PvP clients: short GC pauses instead of big stutters. */
const OPTIMIZED_FLAGS = [
  '-XX:+UseG1GC',
  '-XX:+UnlockExperimentalVMOptions',
  '-XX:G1NewSizePercent=20',
  '-XX:G1ReservePercent=20',
  '-XX:MaxGCPauseMillis=50',
  '-XX:G1HeapRegionSize=32M',
  '-XX:+DisableExplicitGC',
  '-XX:+UseStringDeduplication',
];

/** Optimized first-run game options (only written if options.txt doesn't exist yet). */
const DEFAULT_OPTIONS = [
  'fboEnable:true', 'useVbo:true', 'fancyGraphics:false', 'renderClouds:false', 'particles:1',
  'renderDistance:8', 'maxFps:260', 'enableVsync:false', 'mipmapLevels:0', 'ao:0', 'entityShadows:false',
  'guiScale:2', 'lang:en_US', 'useNativeTransport:true',
];

function joinCp(list) {
  const seen = new Set();
  const out = [];
  for (const p of list) {
    if (seen.has(p)) continue;
    seen.add(p);
    out.push(p);
  }
  return out.join(path.delimiter);
}

/**
 * Makes sure Java, Minecraft, Forge and mods are installed. progress(stageText, fraction 0..1).
 */
async function prepare(settings, progress) {
  const instance = settings.instance || 'default';
  const gameDir = ensureDir(paths.instance(instance));
  ensureDir(paths.cache);

  const java = await ensureJava8(settings.javaPath, progress);
  const vanilla = await installVanilla(MC_VERSION, progress);
  const forge = await installForge(progress);

  progress('Preparing natives', 1);
  extractNatives(vanilla.nativeJars, paths.natives(instance));

  if (settings.syncBundledMods !== false) {
    progress('Updating Pegasus mods', 1);
    mods.syncBundled(instance);
  }

  const optionsFile = path.join(gameDir, 'options.txt');
  if (!fs.existsSync(optionsFile)) fs.writeFileSync(optionsFile, DEFAULT_OPTIONS.join('\n') + '\n');

  return { java, vanilla, forge, gameDir, instance };
}

function buildCommand(settings, prepared, account) {
  const { vanilla, forge, gameDir, instance } = prepared;
  const mem = Math.max(1024, Number(settings.memoryMb) || 3072);
  const jvm = [`-Xmx${mem}M`, `-Xms${Math.min(mem, 1024)}M`];
  if (settings.jvmPreset !== 'vanilla') jvm.push(...OPTIMIZED_FLAGS);
  jvm.push(
    `-Djava.library.path=${paths.natives(instance)}`,
    '-Dfml.ignoreInvalidMinecraftCertificates=true',
    '-Dfml.ignorePatchDiscrepancies=true',
    '-Dlog4j2.formatMsgNoLookups=true',
    '-Dminecraft.launcher.brand=Pegasus',
    '-Dminecraft.launcher.version=1.0.0'
  );
  // Mojang's patched log4j config (fixes the Log4Shell vulnerability in 1.8.9's old log4j).
  if (vanilla.logConfig) jvm.push(vanilla.logConfig.argument.replace('${path}', vanilla.logConfig.file));
  if (settings.extraJvmArgs) jvm.push(...settings.extraJvmArgs.split(/\s+/).filter(Boolean));

  const classpath = joinCp([...forge.classpath, ...vanilla.classpath, vanilla.clientJar]);
  const vars = {
    auth_player_name: account.username,
    version_name: forge.id,
    game_directory: gameDir,
    assets_root: paths.assets,
    assets_index_name: vanilla.assetIndex,
    auth_uuid: account.uuid,
    auth_access_token: account.accessToken,
    user_properties: '{}',
    user_type: account.userType,
  };
  const game = forge.minecraftArguments.split(' ').map((a) => a.replace(/\$\{(\w+)\}/g, (m, k) => (k in vars ? vars[k] : m)));
  game.push('--width', String(settings.width || 1280), '--height', String(settings.height || 720));
  if (settings.fullscreen) game.push('--fullscreen');
  if (settings.server) {
    const [host, port] = settings.server.split(':');
    game.push('--server', host, '--port', port || '25565');
  }
  return { java: prepared.java, args: [...jvm, '-cp', classpath, forge.mainClass, ...game], cwd: gameDir };
}

/** Installs if needed and starts the game. Returns the child process. */
async function launch(settings, { progress, onLog, onExit }, account) {
  account = account || offlineAccount(settings.username);
  const prepared = await prepare(settings, progress);
  const cmd = buildCommand(settings, prepared, account);
  progress('Starting Minecraft', 1);

  ensureDir(paths.logs);
  const logFile = fs.createWriteStream(path.join(paths.logs, 'latest-game.log'));
  const shown = cmd.args.map((a) => (a === account.accessToken && a !== '0' ? '<token>' : a));
  logFile.write(`[Pegasus] ${cmd.java} ${shown.join(' ')}\n`);

  const child = spawn(cmd.java, cmd.args, { cwd: cmd.cwd, windowsHide: false });
  const pipe = (stream) => {
    let buf = '';
    stream.on('data', (d) => {
      logFile.write(d);
      buf += d.toString();
      const lines = buf.split(/\r?\n/);
      buf = lines.pop();
      for (const l of lines) if (onLog) onLog(l);
    });
  };
  pipe(child.stdout);
  pipe(child.stderr);
  child.on('exit', (code) => {
    logFile.end(`\n[Pegasus] Game exited with code ${code}\n`);
    if (onExit) onExit(code);
  });
  child.on('error', (e) => {
    if (onLog) onLog('[Pegasus] Failed to start Java: ' + e.message);
    if (onExit) onExit(-1);
  });
  return child;
}

module.exports = { prepare, buildCommand, launch, MC_VERSION };
