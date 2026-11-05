'use strict';
const { app, BrowserWindow, ipcMain, dialog, shell, safeStorage } = require('electron');
const path = require('path');
const settingsStore = require('./core/settings');
const mods = require('./core/mods');
const { launch } = require('./core/launch');
const { validUsername } = require('./core/auth');
const { paths, ensureDir } = require('./core/paths');

let win;
let game = null;

function createWindow() {
  win = new BrowserWindow({
    width: 1100,
    height: 680,
    minWidth: 900,
    minHeight: 560,
    frame: false,
    backgroundColor: '#0b0d12',
    title: 'Pegasus',
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      // Debug screenshots render offscreen so covered windows still paint.
      offscreen: !!process.env.PEGASUS_SCREENSHOTS,
    },
  });
  win.loadFile(path.join(__dirname, 'ui', 'index.html'));
  win.once('ready-to-show', () => { if (!process.env.PEGASUS_SCREENSHOTS) win.show(); });
  // Debug: PEGASUS_SCREENSHOTS=<dir> saves a PNG of every page and quits.
  if (process.env.PEGASUS_SCREENSHOTS) {
    win.webContents.once('did-finish-load', async () => {
      const fs = require('fs');
      const wait = (ms) => new Promise((r) => setTimeout(r, ms));
      win.setAlwaysOnTop(true);
      win.focus();
      await wait(2500);
      for (const page of ['home', 'mods', 'settings', 'console']) {
        await win.webContents.executeJavaScript(`document.querySelector('.tile[data-page="${page}"]').click()`);
        await wait(900);
        const img = await win.webContents.capturePage();
        fs.writeFileSync(path.join(process.env.PEGASUS_SCREENSHOTS, page + '.png'), img.toPNG());
      }
      if (process.env.PEGASUS_SHOW_ACCOUNT) {
        await win.webContents.executeJavaScript(`document.querySelector('.tile[data-page="home"]').click()`);
        await wait(800);
        await win.webContents.executeJavaScript(`document.getElementById('account').click()`);
        await wait(1500);
        fs.writeFileSync(path.join(process.env.PEGASUS_SCREENSHOTS, 'account.png'), (await win.webContents.capturePage()).toPNG());
      }
      app.quit();
    });
  }
  // Open external links in the browser, never inside the launcher.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
}

function send(channel, data) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, data);
}

app.whenReady().then(createWindow);
app.on('window-all-closed', () => {
  if (!game) app.quit();
});

// ---- window controls ----
ipcMain.on('win:minimize', () => win.minimize());
ipcMain.on('win:maximize', () => (win.isMaximized() ? win.unmaximize() : win.maximize()));
ipcMain.on('win:close', () => win.close());

// ---- settings ----
ipcMain.handle('settings:get', () => settingsStore.load());
ipcMain.handle('settings:set', (e, patch) => {
  const current = settingsStore.load();
  if (patch.username !== undefined && !validUsername(patch.username)) throw new Error('Username must be 3-16 letters, numbers or _');
  return settingsStore.save(Object.assign(current, patch));
});
ipcMain.handle('system:info', () => ({
  totalMemoryMb: Math.floor(require('os').totalmem() / 1048576),
  dataDir: paths.root,
  version: app.getVersion(),
}));

// ---- mods ----
const inst = () => settingsStore.load().instance || 'default';
ipcMain.handle('mods:list', () => {
  mods.syncBundled(inst());
  return mods.list(inst());
});
ipcMain.handle('mods:toggle', (e, file, enabled) => {
  mods.setEnabled(inst(), file, enabled);
  return mods.list(inst());
});
ipcMain.handle('mods:remove', (e, file) => {
  mods.remove(inst(), file);
  return mods.list(inst());
});
ipcMain.handle('mods:add', async () => {
  const r = await dialog.showOpenDialog(win, {
    title: 'Add Forge 1.8.9 mods',
    filters: [{ name: 'Mod jars', extensions: ['jar'] }],
    properties: ['openFile', 'multiSelections'],
  });
  if (!r.canceled) mods.addMods(inst(), r.filePaths);
  return mods.list(inst());
});
ipcMain.handle('mods:addFiles', (e, files) => {
  mods.addMods(inst(), files);
  return mods.list(inst());
});
ipcMain.handle('folder:open', (e, which) => {
  const dir = which === 'mods' ? paths.mods(inst())
    : which === 'screenshots' ? path.join(paths.instance(inst()), 'screenshots')
    : which === 'logs' ? paths.logs
    : paths.instance(inst());
  ensureDir(dir);
  return shell.openPath(dir);
});

// ---- Microsoft account (stored encrypted with Windows DPAPI via safeStorage) ----
const msauth = require('./core/msauth');
const ACCOUNT_FILE = path.join(paths.root, 'account.dat');
let signInCancelled = false;

function loadMsAccount() {
  try {
    if (!require('fs').existsSync(ACCOUNT_FILE) || !safeStorage.isEncryptionAvailable()) return null;
    return JSON.parse(safeStorage.decryptString(require('fs').readFileSync(ACCOUNT_FILE)));
  } catch (e) {
    return null;
  }
}

function saveMsAccount(account) {
  if (!safeStorage.isEncryptionAvailable()) throw new Error('Windows could not encrypt your login; it was not saved.');
  ensureDir(paths.root);
  require('fs').writeFileSync(ACCOUNT_FILE, safeStorage.encryptString(JSON.stringify(account)));
}

function publicAccount(settings) {
  const ms = loadMsAccount();
  if (settings.accountType === 'microsoft' && ms) return { type: 'microsoft', username: ms.username, uuid: ms.uuid };
  return { type: 'offline', username: settings.username };
}

let loginWin = null;

/** Login window: a small window with Microsoft's own page (email, password / 2FA). Resolves with the code. */
function microsoftLoginWindow(clientId) {
  const req = msauth.authorizeRequest(clientId);
  return new Promise((resolve, reject) => {
    // Fresh in-memory session every time: no cookies kept, the launcher only keeps the encrypted refresh token.
    loginWin = new BrowserWindow({
      width: 520,
      height: 680,
      parent: win,
      modal: true,
      title: 'Sign in with Microsoft - Pegasus',
      autoHideMenuBar: true,
      backgroundColor: '#ffffff',
      webPreferences: { partition: 'mslogin-' + Date.now(), contextIsolation: true, nodeIntegration: false, sandbox: true },
    });
    let done = false;
    const finish = (err, code) => {
      if (done) return;
      done = true;
      if (loginWin && !loginWin.isDestroyed()) loginWin.close();
      loginWin = null;
      if (err) reject(err); else resolve({ code, verifier: req.verifier });
    };
    const onRedirect = (url) => {
      if (!url || !url.startsWith(msauth.REDIRECT)) return false;
      try { finish(null, msauth.codeFromRedirect(url, req.state)); } catch (e) { finish(e); }
      return true;
    };
    // Catch the redirect before it loads (it carries the one-time code).
    loginWin.webContents.session.webRequest.onBeforeRequest({ urls: [msauth.REDIRECT + '*'] }, (details, cb) => {
      cb({ cancel: true });
      onRedirect(details.url);
    });
    loginWin.webContents.on('will-redirect', (e, url) => { if (onRedirect(e.url || url)) e.preventDefault(); });
    loginWin.webContents.on('will-navigate', (e, url) => { if (onRedirect(e.url || url)) e.preventDefault(); });
    // Links like "create account" / "forgot password" open in the real browser.
    loginWin.webContents.setWindowOpenHandler(({ url }) => {
      if (/^https:\/\//.test(url)) shell.openExternal(url);
      return { action: 'deny' };
    });
    loginWin.on('closed', () => finish(new Error('Sign-in cancelled')));
    loginWin.loadURL(req.url).catch(() => { /* cancelled redirect or closed window */ });
  });
}

function finishSignIn(settings, account) {
  saveMsAccount(account);
  settingsStore.save(Object.assign(settings, { accountType: 'microsoft' }));
  return publicAccount(settingsStore.load());
}

ipcMain.handle('account:get', () => publicAccount(settingsStore.load()));
ipcMain.handle('account:signIn', async () => {
  const settings = settingsStore.load();
  const clientId = settings.azureClientId.trim();
  const { code, verifier } = await microsoftLoginWindow(clientId);
  send('account:status', 'SIGNING IN TO XBOX AND MINECRAFT...');
  return finishSignIn(settings, await msauth.signInWithCode(clientId, code, verifier));
});
// Fallback: a code typed at microsoft.com/link in the normal browser.
ipcMain.handle('account:signInCode', async () => {
  const settings = settingsStore.load();
  signInCancelled = false;
  const account = await msauth.signIn(settings.azureClientId.trim(), (code) => {
    send('account:code', code);
    shell.openExternal(code.url);
  }, () => signInCancelled);
  return finishSignIn(settings, account);
});
ipcMain.handle('account:cancel', () => {
  signInCancelled = true;
  if (loginWin && !loginWin.isDestroyed()) loginWin.close();
  return true;
});
ipcMain.handle('account:useOffline', () => {
  const s = settingsStore.load();
  settingsStore.save(Object.assign(s, { accountType: 'offline' }));
  return publicAccount(settingsStore.load());
});
ipcMain.handle('account:signOut', () => {
  try { require('fs').unlinkSync(ACCOUNT_FILE); } catch (e) { /* not signed in */ }
  const s = settingsStore.load();
  settingsStore.save(Object.assign(s, { accountType: 'offline' }));
  return publicAccount(settingsStore.load());
});

async function accountForLaunch(settings) {
  if (settings.accountType !== 'microsoft') return null; // offline
  const ms = loadMsAccount();
  if (!ms) throw new Error('You are not signed in with Microsoft anymore. Sign in again or switch to offline.');
  send('game:progress', { text: 'Checking your Microsoft login', p: 0 });
  const fresh = await msauth.ensureFresh(settings.azureClientId.trim(), ms);
  if (fresh !== ms) saveMsAccount(fresh);
  return fresh;
}

// ---- game ----
ipcMain.handle('game:launch', async () => {
  if (game) throw new Error('The game is already running.');
  const settings = settingsStore.load();
  game = 'starting';
  send('game:state', 'installing');
  try {
    let hidden = false;
    const account = await accountForLaunch(settings);
    game = await launch(settings, {
      progress: (text, p) => send('game:progress', { text, p }),
      onLog: (line) => {
        send('game:log', line);
        // Hide once the game window is actually up (LWJGL initialised).
        if (!hidden && settings.afterLaunch === 'hide' && /LWJGL Version|Setting user/.test(line)) {
          hidden = true;
          setTimeout(() => win && !win.isDestroyed() && win.hide(), 1500);
        }
      },
      onExit: (code) => {
        game = null;
        send('game:state', code === 0 ? 'idle' : 'crashed');
        if (win && !win.isDestroyed()) {
          win.show();
          win.focus();
        }
      },
    }, account);
    send('game:state', 'running');
    return true;
  } catch (e) {
    game = null;
    send('game:state', 'idle');
    throw e;
  }
});
// ---- import from Minecraft ----
ipcMain.handle('import:run', () => {
  if (game) throw new Error('Close the game first (it rewrites options.txt when it exits).');
  const report = require('./core/importer').run(inst());
  if (report.fullscreen !== null) settingsStore.save(Object.assign(settingsStore.load(), { fullscreen: report.fullscreen }));
  return report;
});
ipcMain.handle('game:kill', () => {
  if (game && game.kill) game.kill();
  return true;
});
