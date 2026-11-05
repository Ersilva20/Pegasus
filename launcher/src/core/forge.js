'use strict';
const fs = require('fs');
const path = require('path');
const AdmZip = require('adm-zip');
const { paths, ensureDir } = require('./paths');
const { downloadFile, downloadAll } = require('./download');
const { mavenPath } = require('./minecraft');

const FORGE = '1.8.9-11.15.1.2318-1.8.9';
const INSTALLER_URL = `https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE}/forge-${FORGE}-installer.jar`;
const MIRRORS = [
  'https://libraries.minecraft.net/',
  'https://maven.minecraftforge.net/',
  'https://repo1.maven.org/maven2/',
];

/**
 * Installs Forge 1.8.9 the way its installer would for a client: the universal jar goes into
 * libraries, plus Forge's own libraries (scala, launchwrapper, asm...). No installer UI needed.
 */
async function installForge(progress) {
  progress('Checking Forge ' + FORGE, 0);
  const installer = path.join(paths.cache, `forge-${FORGE}-installer.jar`);
  await downloadFile(INSTALLER_URL, installer);
  const zip = new AdmZip(installer);
  const profile = JSON.parse(zip.readAsText('install_profile.json'));
  const info = profile.versionInfo;

  const forgeJar = path.join(paths.libraries, mavenPath(profile.install.path));
  if (!fs.existsSync(forgeJar)) {
    ensureDir(path.dirname(forgeJar));
    fs.writeFileSync(forgeJar, zip.getEntry(profile.install.filePath).getData());
  }

  const classpath = [];
  const tasks = [];
  for (const lib of info.libraries) {
    if (lib.clientreq === false) continue; // server-only (jline)
    const rel = mavenPath(lib.name);
    const dest = path.join(paths.libraries, rel);
    classpath.push(dest);
    if (lib.name.startsWith('net.minecraftforge:forge:')) continue;
    const urlPath = rel.split(path.sep).join('/');
    const urls = [];
    if (lib.url) urls.push(lib.url.replace(/\/?$/, '/') + urlPath);
    for (const m of MIRRORS) if (!urls.includes(m + urlPath)) urls.push(m + urlPath);
    tasks.push({ urls, dest });
  }
  await downloadAll(tasks, (d, t) => progress(`Downloading Forge libraries (${d}/${t})`, d / t), 8);

  return { mainClass: info.mainClass, minecraftArguments: info.minecraftArguments, classpath, id: info.id };
}

module.exports = { installForge, FORGE };
