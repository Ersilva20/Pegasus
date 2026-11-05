'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const AdmZip = require('adm-zip');
const { paths, ensureDir } = require('./paths');

const ASSETS_API = 'https://api.adoptium.net/v3/assets/latest/8/hotspot?os=windows&architecture=x64&image_type=jre&vendor=eclipse';

function findJava(dir) {
  if (!fs.existsSync(dir)) return null;
  const exe = process.platform === 'win32' ? 'javaw.exe' : 'java';
  for (const sub of fs.readdirSync(dir)) {
    const candidate = path.join(dir, sub, 'bin', exe);
    if (fs.existsSync(candidate)) return candidate;
  }
  return null;
}

/** Streams a big download with byte progress and returns its sha256. */
async function streamDownload(url, dest, onProgress) {
  const res = await fetch(url, { headers: { 'User-Agent': 'Pegasus/1.0' } });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  const total = Number(res.headers.get('content-length')) || 0;
  ensureDir(path.dirname(dest));
  const out = fs.createWriteStream(dest + '.part');
  const hash = crypto.createHash('sha256');
  let got = 0;
  const reader = res.body.getReader();
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    const chunk = Buffer.from(value);
    hash.update(chunk);
    got += chunk.length;
    if (!out.write(chunk)) await new Promise((r) => out.once('drain', r));
    if (total && onProgress) onProgress(got / total);
  }
  await new Promise((r) => out.end(r));
  fs.renameSync(dest + '.part', dest);
  return hash.digest('hex');
}

/**
 * Returns a Java 8 executable. Uses the launcher-managed Eclipse Temurin 8 JRE, downloading it
 * (about 40 MB) on first run. A custom path from settings overrides this.
 */
async function ensureJava8(customPath, progress) {
  if (customPath) {
    if (!fs.existsSync(customPath)) throw new Error('Custom Java path does not exist: ' + customPath);
    return customPath;
  }
  const dir = path.join(paths.runtime, 'java8');
  const existing = findJava(dir);
  if (existing) return existing;
  if (process.platform !== 'win32') throw new Error('Automatic Java download is Windows-only for now; set a Java 8 path in Settings.');

  progress('Finding Java 8', 0);
  const res = await fetch(ASSETS_API, { headers: { 'User-Agent': 'Pegasus/1.0' } });
  if (!res.ok) throw new Error('Could not query Adoptium for Java 8: HTTP ' + res.status);
  const list = await res.json();
  const pkg = list[0] && list[0].binary && list[0].binary.package;
  if (!pkg) throw new Error('Adoptium returned no Java 8 package');

  const zipFile = path.join(paths.cache, pkg.name);
  const sha256 = await streamDownload(pkg.link, zipFile, (p) => progress('Downloading Java 8', p));
  if (pkg.checksum && sha256 !== pkg.checksum.toLowerCase()) {
    fs.unlinkSync(zipFile);
    throw new Error('Java download checksum mismatch');
  }
  progress('Installing Java 8', 1);
  ensureDir(dir);
  new AdmZip(zipFile).extractAllTo(dir, true);
  fs.unlinkSync(zipFile);
  const java = findJava(dir);
  if (!java) throw new Error('Java was extracted but javaw.exe was not found');
  return java;
}

module.exports = { ensureJava8 };
