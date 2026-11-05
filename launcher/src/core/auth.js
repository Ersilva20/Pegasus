'use strict';
const crypto = require('crypto');

/**
 * Offline accounts: same UUID scheme Minecraft uses for offline players
 * (UUID v3 of "OfflinePlayer:<name>"). Works for singleplayer and offline-mode servers.
 * Microsoft login is added once the Azure app registration is approved.
 */
function validUsername(name) {
  return /^[A-Za-z0-9_]{3,16}$/.test(name || '');
}

function offlineUuid(name) {
  const md5 = crypto.createHash('md5').update('OfflinePlayer:' + name, 'utf8').digest();
  md5[6] = (md5[6] & 0x0f) | 0x30;
  md5[8] = (md5[8] & 0x3f) | 0x80;
  return md5.toString('hex');
}

function offlineAccount(name) {
  if (!validUsername(name)) throw new Error('Username must be 3-16 letters, numbers or _');
  return { type: 'offline', username: name, uuid: offlineUuid(name), accessToken: '0', userType: 'legacy' };
}

module.exports = { offlineAccount, validUsername, offlineUuid };
