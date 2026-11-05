'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { ensureDir } = require('./paths');

const UA = 'Pegasus/1.0';

function sha1File(file) {
  return new Promise((resolve, reject) => {
    const h = crypto.createHash('sha1');
    fs.createReadStream(file).on('data', (d) => h.update(d)).on('end', () => resolve(h.digest('hex'))).on('error', reject);
  });
}

async function fileOk(file, sha1, size) {
  try {
    const st = fs.statSync(file);
    if (size && st.size !== size) return false;
    if (sha1) return (await sha1File(file)) === sha1.toLowerCase();
    return st.size > 0;
  } catch (e) {
    return false;
  }
}

async function getJson(url) {
  const res = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  return res.json();
}

/** Download one file to dest (via .part), verifying sha1 when given. Tries each url in order. */
async function downloadFile(urls, dest, sha1, size) {
  if (await fileOk(dest, sha1, size)) return false;
  ensureDir(path.dirname(dest));
  const list = Array.isArray(urls) ? urls : [urls];
  let lastErr;
  for (const url of list) {
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        const res = await fetch(url, { headers: { 'User-Agent': UA } });
        if (res.status === 404) throw Object.assign(new Error(`404 ${url}`), { notFound: true });
        if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
        const buf = Buffer.from(await res.arrayBuffer());
        if (sha1) {
          const got = crypto.createHash('sha1').update(buf).digest('hex');
          if (got !== sha1.toLowerCase()) throw new Error(`Checksum mismatch for ${url}`);
        }
        const tmp = dest + '.part';
        fs.writeFileSync(tmp, buf);
        fs.renameSync(tmp, dest);
        return true;
      } catch (e) {
        lastErr = e;
        if (e.notFound) break; // try the next mirror
        await new Promise((r) => setTimeout(r, 500 * (attempt + 1)));
      }
    }
  }
  throw lastErr || new Error(`Could not download ${dest}`);
}

/**
 * Download many files with limited parallelism.
 * tasks: [{ urls, dest, sha1, size }], onProgress(done, total)
 */
async function downloadAll(tasks, onProgress, concurrency = 16) {
  let done = 0;
  let index = 0;
  const total = tasks.length;
  const errors = [];
  async function worker() {
    while (index < tasks.length) {
      const t = tasks[index++];
      try {
        await downloadFile(t.urls, t.dest, t.sha1, t.size);
      } catch (e) {
        errors.push(e);
      }
      done++;
      if (onProgress) onProgress(done, total);
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, Math.max(1, total)) }, worker));
  if (errors.length) throw new Error(`${errors.length} download(s) failed. First: ${errors[0].message}`);
}

module.exports = { getJson, downloadFile, downloadAll, sha1File, fileOk };
