'use strict';
const { contextBridge, ipcRenderer, webUtils } = require('electron');

// The only API the UI can use (no Node access in the page).
contextBridge.exposeInMainWorld('launcher', {
  window: {
    minimize: () => ipcRenderer.send('win:minimize'),
    maximize: () => ipcRenderer.send('win:maximize'),
    close: () => ipcRenderer.send('win:close'),
  },
  settings: {
    get: () => ipcRenderer.invoke('settings:get'),
    set: (patch) => ipcRenderer.invoke('settings:set', patch),
  },
  system: () => ipcRenderer.invoke('system:info'),
  account: {
    get: () => ipcRenderer.invoke('account:get'),
    signIn: () => ipcRenderer.invoke('account:signIn'),
    signInCode: () => ipcRenderer.invoke('account:signInCode'),
    onStatus: (cb) => ipcRenderer.on('account:status', (e, d) => cb(d)),
    cancel: () => ipcRenderer.invoke('account:cancel'),
    useOffline: () => ipcRenderer.invoke('account:useOffline'),
    signOut: () => ipcRenderer.invoke('account:signOut'),
    onCode: (cb) => ipcRenderer.on('account:code', (e, d) => cb(d)),
  },
  mods: {
    list: () => ipcRenderer.invoke('mods:list'),
    toggle: (file, enabled) => ipcRenderer.invoke('mods:toggle', file, enabled),
    remove: (file) => ipcRenderer.invoke('mods:remove', file),
    add: () => ipcRenderer.invoke('mods:add'),
    addFiles: (files) => ipcRenderer.invoke('mods:addFiles', files.map((f) => webUtils.getPathForFile(f))),
  },
  openFolder: (which) => ipcRenderer.invoke('folder:open', which),
  importSetup: () => ipcRenderer.invoke('import:run'),
  game: {
    launch: () => ipcRenderer.invoke('game:launch'),
    kill: () => ipcRenderer.invoke('game:kill'),
    onProgress: (cb) => ipcRenderer.on('game:progress', (e, d) => cb(d)),
    onLog: (cb) => ipcRenderer.on('game:log', (e, d) => cb(d)),
    onState: (cb) => ipcRenderer.on('game:state', (e, d) => cb(d)),
  },
});
