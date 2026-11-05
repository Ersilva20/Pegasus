'use strict';
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const { paths, ensureDir } = require('./paths');
const { getJson, downloadFile, downloadAll } = require('./download');

const MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const RESOURCES = 'https://resources.download.minecraft.net/';

function osName() {
  return process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'osx' : 'linux';
}

/** Mojang library rules: last matching rule wins; no rules = allowed. */
function rulesAllow(rules) {
  if (!rules || !rules.length) return true;
  let allowed = false;
  for (const r of rules) {
    const matches = !r.os || r.os.name === osName();
    if (matches) allowed = r.action === 'allow';
  }
  return allowed;
}

/** "group:artifact:version[:classifier]" -> relative jar path */
function mavenPath(name) {
  const [group, artifact, version, classifier] = name.split(':');
  const file = `${artifact}-${version}${classifier ? '-' + classifier : ''}.jar`;
  return path.join(...group.split('.'), artifact, version, file);
}

async function versionJson(id) {
  const file = path.join(paths.versions, id, id + '.json');
  if (fs.existsSync(file)) return JSON.parse(fs.readFileSync(file, 'utf8'));
  const manifest = await getJson(MANIFEST);
  const entry = manifest.versions.find((v) => v.id === id);
  if (!entry) throw new Error(`Minecraft ${id} not found in Mojang's version list`);
  await downloadFile(entry.url, file, entry.sha1);
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

/**
 * Downloads client jar, libraries, natives, assets and log config for a vanilla version.
 * Returns what the launcher needs to build the command line.
 */
async function installVanilla(id, progress) {
  progress('Checking Minecraft ' + id, 0);
  const v = await versionJson(id);

  const clientJar = path.join(paths.versions, id, id + '.jar');
  const tasks = [{ urls: v.downloads.client.url, dest: clientJar, sha1: v.downloads.client.sha1, size: v.downloads.client.size }];
  const classpath = [];
  const nativeJars = [];

  for (const lib of v.libraries) {
    if (!rulesAllow(lib.rules)) continue;
    const dl = lib.downloads || {};
    if (dl.artifact) {
      const dest = path.join(paths.libraries, dl.artifact.path);
      tasks.push({ urls: dl.artifact.url, dest, sha1: dl.artifact.sha1, size: dl.artifact.size });
      classpath.push(dest);
    }
    if (lib.natives && lib.natives[osName()] && dl.classifiers) {
      const key = lib.natives[osName()].replace('${arch}', process.arch === 'ia32' ? '32' : '64');
      const nat = dl.classifiers[key];
      if (nat) {
        const dest = path.join(paths.libraries, nat.path);
        tasks.push({ urls: nat.url, dest, sha1: nat.sha1, size: nat.size });
        nativeJars.push({ file: dest, exclude: (lib.extract && lib.extract.exclude) || ['META-INF/'] });
      }
    }
  }

  let logConfig = null;
  if (v.logging && v.logging.client && v.logging.client.file) {
    const f = v.logging.client.file;
    logConfig = { file: path.join(paths.assets, 'log_configs', f.id), argument: v.logging.client.argument };
    tasks.push({ urls: f.url, dest: logConfig.file, sha1: f.sha1, size: f.size });
  }

  await downloadAll(tasks, (d, t) => progress(`Downloading game files (${d}/${t})`, d / t));

  // Assets (sounds, languages, textures)
  const indexFile = path.join(paths.assets, 'indexes', v.assetIndex.id + '.json');
  await downloadFile(v.assetIndex.url, indexFile, v.assetIndex.sha1);
  const index = JSON.parse(fs.readFileSync(indexFile, 'utf8'));
  const assetTasks = Object.values(index.objects).map((o) => {
    const sub = o.hash.substring(0, 2);
    return { urls: RESOURCES + sub + '/' + o.hash, dest: path.join(paths.assets, 'objects', sub, o.hash), sha1: o.hash, size: o.size };
  });
  await downloadAll(assetTasks, (d, t) => progress(`Downloading assets (${d}/${t})`, d / t), 24);

  return { version: v, clientJar, classpath, nativeJars, logConfig, assetIndex: v.assetIndex.id };
}

/** Unzips native libraries (LWJGL, OpenAL, ...) into the instance's natives folder. */
function extractNatives(nativeJars, dir) {
  ensureDir(dir);
  for (const n of nativeJars) {
    const zip = new AdmZip(n.file);
    for (const e of zip.getEntries()) {
      if (e.isDirectory || n.exclude.some((x) => e.entryName.startsWith(x))) continue;
      const out = path.join(dir, e.entryName);
      if (fs.existsSync(out) && fs.statSync(out).size === e.header.size) continue;
      ensureDir(path.dirname(out));
      fs.writeFileSync(out, e.getData());
    }
  }
}

module.exports = { installVanilla, extractNatives, mavenPath, rulesAllow };
