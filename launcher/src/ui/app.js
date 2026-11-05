'use strict';
const L = window.launcher;
const $ = (id) => document.getElementById(id);
PixelIcons.applyIcons();

let settings = {};
let modsCache = [];
let modFilter = 'all';
let gameState = 'idle';
let account = { type: 'offline', username: 'Player' };
let maxMemory = 8192;

// ---------- helpers ----------
function toast(msg, error) {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'toast px' + (error ? ' error' : '');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => t.classList.add('hidden'), 4500);
}
function errMsg(e) {
  return String(e && e.message ? e.message : e).replace(/^Error invoking remote method '[^']+': (Error: )?/, '');
}
function makeSeg(el, cells) {
  el.textContent = '';
  for (let i = 0; i < cells; i++) el.appendChild(document.createElement('i'));
}
function setSeg(el, fraction) {
  const cells = el.children;
  const on = Math.round(Math.max(0, Math.min(1, fraction)) * cells.length);
  for (let i = 0; i < cells.length; i++) cells[i].classList.toggle('on', i < on);
}

// ---------- clock + greeting ----------
function tick() {
  const d = new Date();
  const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  let h = d.getHours();
  const ampm = h >= 12 ? 'PM' : 'AM';
  h = h % 12 || 12;
  $('clock').textContent = `${days[d.getDay()]} ${String(h).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}${ampm}`;
  renderGreeting();
}
function renderGreeting() {
  const hr = new Date().getHours();
  const part = hr < 5 ? 'GOOD NIGHT' : hr < 12 ? 'GOOD MORNING' : hr < 18 ? 'GOOD AFTERNOON' : 'GOOD EVENING';
  const name = account.type === 'microsoft' ? account.username : settings.username || 'PLAYER';
  $('greeting').textContent = `${part}, ${String(name).toUpperCase()}`;
}

// ---------- pixel landscape (procedural, changes with time of day) ----------
function drawScene() {
  const c = $('scene');
  const g = c.getContext('2d');
  const W = c.width, H = c.height;
  let seed = 1337;
  const rnd = () => ((seed = (seed * 16807) % 2147483647) / 2147483647);
  const hr = new Date().getHours();
  const night = hr < 6 || hr >= 20, dusk = !night && (hr >= 17 || hr < 8);
  const sky = night ? ['#0b0f2a', '#121a3d', '#1b2550', '#243063'] : dusk ? ['#2b2a63', '#6b3f7a', '#c4607a', '#f0a070'] : ['#3a7bd5', '#5a97e0', '#7cb3ea', '#a4cdf2'];
  const band = Math.ceil(H * 0.62 / sky.length);
  sky.forEach((col, i) => { g.fillStyle = col; g.fillRect(0, i * band, W, band); });
  // dithered band edges
  for (let i = 1; i < sky.length; i++) {
    g.fillStyle = sky[i];
    for (let x = (i % 2); x < W; x += 2) g.fillRect(x, i * band - 1, 1, 1);
  }
  if (night) {
    g.fillStyle = '#fff6e0';
    for (let i = 0; i < 40; i++) g.fillRect(Math.floor(rnd() * W), Math.floor(rnd() * H * 0.5), 1, 1);
    g.fillStyle = '#e8e4d0'; g.fillRect(160, 7, 7, 7); g.fillStyle = '#c9c4ad'; g.fillRect(162, 9, 2, 2); g.fillRect(165, 11, 1, 1);
  } else {
    g.fillStyle = dusk ? '#ffd27a' : '#fff3a0';
    g.fillRect(dusk ? 150 : 158, dusk ? 20 : 6, 9, 9);
    g.fillStyle = dusk ? '#ffb347' : '#ffe066';
    g.fillRect((dusk ? 150 : 158) + 2, (dusk ? 20 : 6) + 2, 5, 5);
  }
  // clouds
  g.fillStyle = night ? 'rgba(200,210,255,.18)' : dusk ? 'rgba(255,220,220,.55)' : 'rgba(255,255,255,.85)';
  [[18, 9, 22], [70, 14, 16], [118, 6, 26], [182, 16, 14]].forEach(([x, y, w]) => {
    g.fillRect(x, y, w, 3); g.fillRect(x + 3, y - 2, w - 8, 2); g.fillRect(x + 6, y - 3, w - 14, 1);
  });
  // far hills
  const hill = night ? '#1d2a33' : dusk ? '#4a3a5a' : '#5b8f5a';
  g.fillStyle = hill;
  for (let x = 0; x < W; x++) {
    const h = Math.floor(8 + 5 * Math.sin(x / 14) + 3 * Math.sin(x / 5.3));
    g.fillRect(x, 34 - h, 1, h + 4);
  }
  // ground: grass top + dirt with speckles
  const groundY = 38;
  const grass = night ? ['#1f6a2c', '#174f21'] : ['#3ccb4f', '#23913a'];
  const dirt = night ? ['#4a3022', '#36231a'] : ['#8a5a3b', '#6b432b'];
  for (let x = 0; x < W; x++) {
    const bump = (Math.floor(x / 8) % 3 === 0) ? 1 : 0;
    g.fillStyle = grass[0]; g.fillRect(x, groundY - bump, 1, 3 + bump);
    g.fillStyle = grass[1]; if (rnd() < 0.5) g.fillRect(x, groundY + 2, 1, 1 + (rnd() < 0.3 ? 1 : 0));
    g.fillStyle = dirt[0]; g.fillRect(x, groundY + 3, 1, H - groundY);
  }
  g.fillStyle = dirt[1];
  for (let i = 0; i < 160; i++) g.fillRect(Math.floor(rnd() * W), groundY + 4 + Math.floor(rnd() * (H - groundY - 4)), 1, 1);
  // trees
  const leaf = night ? ['#1b5a26', '#133f1b'] : ['#2fae44', '#1f7d32'];
  [[24, 7], [58, 6], [140, 8], [186, 6]].forEach(([x, h]) => {
    g.fillStyle = night ? '#3a2616' : '#6b4a2b'; g.fillRect(x, groundY - h, 2, h);
    g.fillStyle = leaf[0]; g.fillRect(x - 4, groundY - h - 6, 10, 6); g.fillRect(x - 2, groundY - h - 8, 6, 2);
    g.fillStyle = leaf[1]; g.fillRect(x - 4, groundY - h - 1, 10, 1); g.fillRect(x + 3, groundY - h - 5, 2, 2);
  });
}

// ---------- navigation ----------
function show(page) {
  document.querySelectorAll('.tile[data-page]').forEach((b) => b.classList.toggle('active', b.dataset.page === page));
  document.querySelectorAll('.page').forEach((p) => p.classList.toggle('active', p.id === 'page-' + page));
  if (page === 'mods') refreshMods();
}
document.querySelectorAll('.tile[data-page]').forEach((b) => b.addEventListener('click', () => show(b.dataset.page)));
document.querySelectorAll('[data-goto]').forEach((c) => c.addEventListener('click', () => show(c.dataset.goto)));
$('navFolder').onclick = () => L.openFolder('game');
$('openScreens').onclick = () => L.openFolder('screenshots');
$('openMods').onclick = () => L.openFolder('mods');
$('btnMin').onclick = () => L.window.minimize();
$('btnMax').onclick = () => L.window.maximize();
$('btnClose').onclick = () => L.window.close();

// ---------- account ----------
async function refreshAccount() {
  account = await L.account.get();
  renderAccount();
}
function renderAccount() {
  const ms = account.type === 'microsoft';
  $('accountName').textContent = String(ms ? account.username : settings.username).toUpperCase();
  $('accountBadge').textContent = ms ? 'MICROSOFT' : 'OFFLINE';
  $('accountBadge').classList.toggle('on', ms);
  $('msState').textContent = ms ? 'SIGNED IN: ' + account.username.toUpperCase() : 'NOT SIGNED IN';
  $('msState').classList.toggle('on', ms);
  $('msSignOut').classList.toggle('hidden', !ms);
  $('msSignIn').textContent = ms ? 'SWITCH ACCOUNT' : 'SIGN IN WITH MICROSOFT';
  renderGreeting();
}
function renderMsSetup() {
  $('msSetup').classList.toggle('hidden', !!settings.azureClientId);
  $('msClientInput').value = settings.azureClientId || '';
}
$('account').onclick = () => {
  $('userInput').value = settings.username;
  $('msCodeBox').classList.add('hidden');
  $('msStatus').classList.add('hidden');
  renderMsSetup();
  $('userModal').classList.remove('hidden');
};
$('msClientSave').onclick = async () => {
  const id = $('msClientInput').value.trim();
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id)) {
    toast('That does not look like a Client ID (00000000-0000-0000-0000-000000000000).', true);
    return;
  }
  settings = await L.settings.set({ azureClientId: id });
  $('sClient').value = id;
  renderMsSetup();
  toast('Client ID saved. Now click SIGN IN WITH MICROSOFT.');
};
$('msGuide').onclick = (e) => { e.preventDefault(); window.open('https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade'); };
L.account.onStatus((text) => {
  $('msStatus').textContent = text;
  $('msStatus').classList.remove('hidden');
});
$('userCancel').onclick = () => $('userModal').classList.add('hidden');

async function useOffline() {
  try {
    settings = await L.settings.set({ username: $('userInput').value.trim() });
    account = await L.account.useOffline();
    $('userModal').classList.add('hidden');
    renderAccount();
    $('sUser').value = settings.username;
    toast('Playing offline as ' + settings.username);
  } catch (e) {
    toast(errMsg(e), true);
  }
}
$('userSave').onclick = useOffline;
$('userInput').addEventListener('keydown', (e) => { if (e.key === 'Enter') useOffline(); });

L.account.onCode(({ userCode, url }) => {
  $('msCode').textContent = userCode;
  $('msUrl').textContent = url.replace(/^https?:\/\//, '');
  $('msUrl').onclick = (e) => { e.preventDefault(); window.open(url); };
  $('msCodeBox').classList.remove('hidden');
});
async function msSignIn(withCode) {
  if (!settings.azureClientId) {
    toast('First paste your Azure Client ID (one-time setup above).', true);
    renderMsSetup();
    $('msClientInput').focus();
    return;
  }
  $('msSignIn').disabled = true;
  $('msCancel').classList.remove('hidden');
  $('msStatus').textContent = withCode ? 'WAITING FOR THE CODE...' : 'WAITING FOR THE MICROSOFT WINDOW...';
  $('msStatus').classList.remove('hidden');
  try {
    account = await (withCode ? L.account.signInCode() : L.account.signIn());
    settings = await L.settings.get();
    renderAccount();
    $('userModal').classList.add('hidden');
    toast('Signed in as ' + account.username);
  } catch (e) {
    toast(errMsg(e), true);
  } finally {
    $('msSignIn').disabled = false;
    $('msCancel').classList.add('hidden');
    $('msCodeBox').classList.add('hidden');
    $('msStatus').classList.add('hidden');
  }
}
$('msSignIn').onclick = () => msSignIn(false);
$('msUseCode').onclick = (e) => { e.preventDefault(); msSignIn(true); };
$('msCancel').onclick = () => L.account.cancel();
$('msSignOut').onclick = async () => {
  account = await L.account.signOut();
  settings = await L.settings.get();
  renderAccount();
  toast('Signed out. Playing offline.');
};

// ---------- home / play ----------
makeSeg($('progress'), 24);
function renderStats() {
  $('statMem').textContent = (settings.memoryMb / 1024).toFixed(1) + ' GB';
  $('statMods').textContent = modsCache.length ? modsCache.filter((m) => m.enabled).length : '--';
  $('quickServer').value = settings.server || '';
}
$('quickServer').addEventListener('change', async (e) => { settings = await L.settings.set({ server: e.target.value.trim() }); });

function setPlay(state, text) {
  gameState = state;
  const btn = $('play');
  btn.classList.toggle('running', state === 'running');
  btn.disabled = state === 'installing';
  $('playLabel').textContent = state === 'installing' ? 'WAIT' : state === 'running' ? 'PLAYING' : 'PLAY';
  if (text) $('playStatus').textContent = text.toUpperCase();
  if (state !== 'installing') setSeg($('progress'), state === 'running' ? 1 : 0);
}
$('play').onclick = async () => {
  if (gameState === 'running') return show('console');
  setPlay('installing', 'Starting...');
  try {
    await L.game.launch();
  } catch (e) {
    setPlay('idle', 'Launch failed');
    toast(errMsg(e), true);
  }
};
L.game.onProgress(({ text, p }) => {
  $('playStatus').textContent = String(text).toUpperCase();
  setSeg($('progress'), p || 0);
});
L.game.onState((state) => {
  if (state === 'running') setPlay('running', 'Minecraft is running');
  else if (state === 'crashed') {
    setPlay('idle', 'Game closed with an error - see console');
    toast('Minecraft closed with an error. Check the Console tab.', true);
  } else if (state === 'idle') setPlay('idle', 'Ready to play');
});

// ---------- console ----------
const consoleEl = $('console');
let logLines = 0;
L.game.onLog((line) => {
  const m = line.match(/<!\[CDATA\[(.*)\]\]>/);
  if (/<\/?log4j:|^\s*$/.test(line) && !m) return;
  const text = m ? m[1] : line;
  const span = document.createElement('span');
  span.textContent = text + '\n';
  if (/ERROR|Exception|FATAL/.test(line)) span.className = 'err';
  else if (/WARN/.test(line)) span.className = 'warn';
  else if (/Pegasus|MyLauncher|\[ML\]/.test(text)) span.className = 'ml';
  const atBottom = consoleEl.scrollTop + consoleEl.clientHeight >= consoleEl.scrollHeight - 20;
  consoleEl.appendChild(span);
  if (++logLines > 3000) { consoleEl.removeChild(consoleEl.firstChild); logLines--; }
  if (atBottom) consoleEl.scrollTop = consoleEl.scrollHeight;
});
$('clearLog').onclick = () => { consoleEl.textContent = ''; logLines = 0; };
$('copyLog').onclick = () => { navigator.clipboard.writeText(consoleEl.textContent); toast('Console copied'); };
$('logsFolder').onclick = () => L.openFolder('logs');
$('killGame').onclick = () => L.game.kill();

// ---------- mods ----------
const ICON_COLORS = ['#d9823b', '#3ccb4f', '#3fb5f0', '#a87be8', '#d6403a', '#f2c14e', '#2bb5a0', '#e070b0'];
function colorFor(name) {
  let h = 0;
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return ICON_COLORS[h % ICON_COLORS.length];
}
function modMatches(m) {
  const q = $('modSearch').value.trim().toLowerCase();
  if (q && !(m.name + ' ' + m.description + ' ' + m.file).toLowerCase().includes(q)) return false;
  if (modFilter === 'yours') return !m.builtIn;
  if (modFilter === 'builtin') return m.builtIn;
  if (modFilter === 'disabled') return !m.enabled;
  return true;
}
function renderMods() {
  const list = $('modList');
  list.textContent = '';
  const on = modsCache.filter((m) => m.enabled).length;
  $('modCount').textContent = `${on} / ${modsCache.length} ON`;
  const shown = modsCache.filter(modMatches);
  if (!shown.length) {
    const e = document.createElement('div');
    e.className = 'empty';
    e.textContent = modFilter === 'yours' ? 'NO MODS OF YOUR OWN YET - CLICK "+ ADD MODS" OR DROP .JAR FILES ABOVE' : 'NO MODS FOUND';
    list.appendChild(e);
  }
  for (const m of shown) {
    const row = document.createElement('div');
    row.className = 'mod' + (m.enabled ? '' : ' off');
    const icon = document.createElement('div');
    icon.className = 'mod-icon';
    icon.style.background = colorFor(m.name);
    icon.textContent = m.name.replace(/[^A-Za-z0-9]/g, '').slice(0, 2).toUpperCase() || '?';
    const main = document.createElement('div');
    main.className = 'mod-main';
    const name = document.createElement('div');
    name.className = 'mod-name';
    const nameText = document.createElement('span');
    nameText.textContent = m.name;
    name.appendChild(nameText);
    if (m.required || !m.builtIn) {
      const t = document.createElement('span');
      t.className = 'rank ' + (m.required ? 'req' : 'yours');
      t.textContent = m.required ? 'REQUIRED' : 'YOURS';
      name.appendChild(t);
    }
    const desc = document.createElement('div');
    desc.className = 'mod-desc';
    desc.textContent = m.description || m.file;
    desc.title = m.description || m.file;
    main.append(name, desc);
    row.append(icon, main);
    if (!m.builtIn) {
      const del = document.createElement('button');
      del.className = 'del';
      del.title = 'Remove mod';
      del.textContent = 'X';
      del.onclick = async () => {
        if (!confirm('Remove ' + m.file + '?')) return;
        modsCache = await L.mods.remove(m.file);
        renderMods();
        renderStats();
      };
      row.appendChild(del);
    }
    const tg = document.createElement('button');
    tg.className = 'ptoggle' + (m.enabled ? ' on' : '');
    tg.title = m.required ? 'Required by the other Pegasus mods' : m.enabled ? 'Click to disable' : 'Click to enable';
    tg.disabled = m.required;
    const img = document.createElement('img');
    img.className = 'pxi';
    img.src = PixelIcons.iconUrl(m.enabled ? 'check' : 'cross');
    tg.appendChild(img);
    tg.onclick = async () => {
      try {
        modsCache = await L.mods.toggle(m.file, !m.enabled);
        renderMods();
        renderStats();
      } catch (e) {
        toast(errMsg(e), true);
      }
    };
    row.appendChild(tg);
    list.appendChild(row);
  }
}
async function refreshMods() {
  try { modsCache = await L.mods.list(); } catch (e) { toast(errMsg(e), true); }
  renderMods();
  renderStats();
}
$('modSearch').addEventListener('input', renderMods);
document.querySelectorAll('#modTabs .ptab').forEach((t) => t.addEventListener('click', () => {
  document.querySelectorAll('#modTabs .ptab').forEach((x) => x.classList.toggle('active', x === t));
  modFilter = t.dataset.filter;
  renderMods();
}));
$('addMods').onclick = async () => {
  const before = modsCache.length;
  modsCache = await L.mods.add();
  renderMods();
  renderStats();
  if (modsCache.length > before) toast(`Added ${modsCache.length - before} mod(s)`);
};
$('modsFolder').onclick = () => L.openFolder('mods');
const dz = $('dropzone');
['dragenter', 'dragover'].forEach((ev) => document.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add('over'); }));
['dragleave', 'drop'].forEach((ev) => document.addEventListener(ev, (e) => { e.preventDefault(); if (ev === 'drop' || e.target === dz) dz.classList.remove('over'); }));
document.addEventListener('drop', async (e) => {
  const files = [...e.dataTransfer.files].filter((f) => f.name.toLowerCase().endsWith('.jar'));
  if (!files.length) return toast('Only .jar mod files can be added', true);
  modsCache = await L.mods.addFiles(files);
  show('mods');
  toast(`Added ${files.length} mod(s)`);
});

// ---------- settings ----------
makeSeg($('memBar'), 16);
function renderMemory(mb) {
  $('memLabel').textContent = (mb / 1024).toFixed(1) + ' GB';
  setSeg($('memBar'), (mb - 1024) / (maxMemory - 1024));
}
function setCheck(id, on) { $(id).classList.toggle('on', !!on); }
function bindSettings() {
  $('sUser').value = settings.username;
  renderMemory(settings.memoryMb);
  $('sPreset').value = settings.jvmPreset;
  $('sJvm').value = settings.extraJvmArgs;
  $('sW').value = settings.width;
  $('sH').value = settings.height;
  setCheck('sFull', settings.fullscreen);
  $('sAfter').value = settings.afterLaunch === 'keep' ? 'keep' : 'hide';
  $('sJava').value = settings.javaPath;
  $('sClient').value = settings.azureClientId || '';
  setCheck('sSync', settings.syncBundledMods !== false);
}
async function saveSetting(patch) {
  try {
    settings = await L.settings.set(patch);
    renderStats();
    renderAccount();
    const s = $('saved');
    s.classList.add('show');
    clearTimeout(saveSetting.t);
    saveSetting.t = setTimeout(() => s.classList.remove('show'), 1200);
  } catch (e) {
    toast(errMsg(e), true);
    bindSettings();
  }
}
function stepMemory(dir) {
  const mb = Math.max(1024, Math.min(maxMemory, settings.memoryMb + dir * 512));
  renderMemory(mb);
  saveSetting({ memoryMb: mb });
}
$('memMinus').onclick = () => stepMemory(-1);
$('memPlus').onclick = () => stepMemory(1);
$('memBar').addEventListener('click', (e) => {
  const r = $('memBar').getBoundingClientRect();
  const f = Math.max(0, Math.min(1, (e.clientX - r.left) / r.width));
  const mb = Math.round((1024 + f * (maxMemory - 1024)) / 512) * 512;
  renderMemory(mb);
  saveSetting({ memoryMb: mb });
});
$('sUser').addEventListener('change', (e) => saveSetting({ username: e.target.value.trim() }));
$('sPreset').addEventListener('change', (e) => saveSetting({ jvmPreset: e.target.value }));
$('sJvm').addEventListener('change', (e) => saveSetting({ extraJvmArgs: e.target.value.trim() }));
$('sW').addEventListener('change', (e) => saveSetting({ width: Number(e.target.value) || 1280 }));
$('sH').addEventListener('change', (e) => saveSetting({ height: Number(e.target.value) || 720 }));
$('sFull').onclick = () => { setCheck('sFull', !settings.fullscreen); saveSetting({ fullscreen: !settings.fullscreen }); };
$('sAfter').addEventListener('change', (e) => saveSetting({ afterLaunch: e.target.value }));
$('sJava').addEventListener('change', (e) => saveSetting({ javaPath: e.target.value.trim() }));
$('sClient').addEventListener('change', (e) => saveSetting({ azureClientId: e.target.value.trim() }));
$('sSync').onclick = () => { const v = settings.syncBundledMods === false; setCheck('sSync', v); saveSetting({ syncBundledMods: v }); };
$('importBtn').onclick = async () => {
  const btn = $('importBtn');
  btn.disabled = true;
  btn.textContent = 'IMPORTING...';
  try {
    const r = await L.importSetup();
    settings = await L.settings.get();
    bindSettings();
    const parts = [];
    if (r.options) parts.push('options + keybinds');
    parts.push(r.servers + ' server(s)', r.packs.length + ' texture pack(s)', r.screenshots + ' screenshot(s)', r.mods + ' mod setting(s)');
    let text = 'Imported: ' + parts.join(', ') + '.';
    if (r.skippedPacks.length) text += ' Not imported (.rar, extract them into a folder first): ' + r.skippedPacks.join(', ') + '.';
    $('importResult').textContent = text;
    toast('Import done.');
  } catch (e) {
    toast(errMsg(e), true);
  } finally {
    btn.disabled = false;
    btn.textContent = 'IMPORT FROM LUNAR / MINECRAFT';
  }
};

// ---------- start ----------
(async () => {
  settings = await L.settings.get();
  const info = await L.system();
  maxMemory = Math.max(2048, Math.min(16384, Math.floor(info.totalMemoryMb * 0.75 / 512) * 512));
  $('dataDir').textContent = 'Data: ' + info.dataDir + ' · v' + info.version;
  bindSettings();
  renderStats();
  await refreshAccount();
  drawScene();
  tick();
  setInterval(tick, 10000);
  setInterval(drawScene, 10 * 60 * 1000);
  refreshMods();
})();
