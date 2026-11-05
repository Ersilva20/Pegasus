'use strict';
/**
 * Microsoft account login for Minecraft (official chain):
 *   Microsoft OAuth (login window, or device code) -> Xbox Live -> XSTS -> Minecraft token -> profile.
 * Needs YOUR Azure app's client ID (Settings > Advanced). New apps must also be approved by Microsoft
 * for the Minecraft API; until then the Minecraft step returns HTTP 403.
 */
const MS = 'https://login.microsoftonline.com/consumers/oauth2/v2.0';
const SCOPE = 'XboxLive.signin offline_access';
// Redirect URI of the login window. Add it in Azure: Authentication > Mobile and desktop applications.
const REDIRECT = 'https://login.microsoftonline.com/common/oauth2/nativeclient';
const crypto = require('crypto');

async function form(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(body).toString(),
  });
  return { status: res.status, data: await res.json().catch(() => ({})) };
}

async function json(url, body, headers = {}) {
  const res = await fetch(url, {
    method: body ? 'POST' : 'GET',
    headers: Object.assign({ 'Content-Type': 'application/json', Accept: 'application/json' }, headers),
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: res.status, data: await res.json().catch(() => ({})) };
}

/** Step 1: ask Microsoft for a code the user types at microsoft.com/link. */
async function startDeviceCode(clientId) {
  if (!clientId) throw new Error('Set your Azure Client ID in Settings > Advanced first.');
  const { status, data } = await form(MS + '/devicecode', { client_id: clientId, scope: SCOPE });
  if (status !== 200) throw new Error('Microsoft rejected the Client ID: ' + (data.error_description || data.error || status));
  return { userCode: data.user_code, deviceCode: data.device_code, url: data.verification_uri, interval: data.interval || 5, expiresIn: data.expires_in };
}

/** Step 2: wait until the user approved the code; returns Microsoft tokens. */
async function pollDeviceCode(clientId, device, isCancelled) {
  let interval = device.interval * 1000;
  const deadline = Date.now() + device.expiresIn * 1000;
  while (Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, interval));
    if (isCancelled && isCancelled()) throw new Error('Sign-in cancelled');
    const { data } = await form(MS + '/token', {
      grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
      client_id: clientId,
      device_code: device.deviceCode,
    });
    if (data.access_token) return data;
    if (data.error === 'authorization_pending') continue;
    if (data.error === 'slow_down') { interval += 5000; continue; }
    if (data.error === 'authorization_declined') throw new Error('Sign-in was declined');
    if (data.error === 'expired_token') break;
    throw new Error('Microsoft sign-in failed: ' + (data.error_description || data.error));
  }
  throw new Error('The code expired. Try signing in again.');
}

async function refreshMicrosoft(clientId, refreshToken) {
  const { data } = await form(MS + '/token', { grant_type: 'refresh_token', client_id: clientId, refresh_token: refreshToken, scope: SCOPE });
  if (!data.access_token) throw new Error('Your Microsoft session expired, please sign in again.');
  return data;
}

/** PKCE pair + the Microsoft page where the user types email, password / 2FA (shown in the login window). */
function authorizeRequest(clientId) {
  if (!clientId) throw new Error('Set your Azure Client ID first.');
  const verifier = crypto.randomBytes(32).toString('base64url');
  const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');
  const state = crypto.randomBytes(16).toString('hex');
  const url = MS + '/authorize?' + new URLSearchParams({
    client_id: clientId,
    response_type: 'code',
    redirect_uri: REDIRECT,
    scope: SCOPE,
    code_challenge: challenge,
    code_challenge_method: 'S256',
    state,
    prompt: 'select_account',
  }).toString();
  return { url, verifier, state };
}

/** Reads the redirect the login window landed on. Returns the code, or throws a readable error. */
function codeFromRedirect(redirectUrl, state) {
  const q = new URL(redirectUrl).searchParams;
  if (q.get('error')) {
    if (q.get('error') === 'access_denied') throw new Error('Sign-in cancelled');
    const desc = q.get('error_description') || q.get('error');
    throw new Error('Microsoft sign-in failed: ' + desc);
  }
  if (q.get('state') !== state || !q.get('code')) throw new Error('Sign-in was interrupted, please try again.');
  return q.get('code');
}

async function exchangeCode(clientId, code, verifier) {
  const { data } = await form(MS + '/token', {
    grant_type: 'authorization_code', client_id: clientId, code, redirect_uri: REDIRECT, code_verifier: verifier, scope: SCOPE,
  });
  if (!data.access_token) throw new Error('Microsoft sign-in failed: ' + (data.error_description || data.error || 'no token'));
  return data;
}

const XSTS_ERRORS = {
  2148916233: 'This Microsoft account has no Xbox profile. Sign in once at minecraft.net to create one.',
  2148916235: 'Xbox Live is not available in your country.',
  2148916236: 'This account needs adult verification on xbox.com.',
  2148916237: 'This account needs adult verification on xbox.com.',
  2148916238: 'This is a child account; it must be added to a Microsoft family to play.',
};

/** Steps 3-6: Microsoft token -> Minecraft account. */
async function minecraftFromMicrosoft(msAccessToken) {
  const xbl = await json('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: 'd=' + msAccessToken },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT',
  });
  if (xbl.status !== 200) throw new Error('Xbox Live login failed (HTTP ' + xbl.status + ')');
  const uhs = xbl.data.DisplayClaims.xui[0].uhs;

  const xsts = await json('https://xsts.auth.xboxlive.com/xsts/authorize', {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.data.Token] },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT',
  });
  if (xsts.status !== 200) throw new Error(XSTS_ERRORS[xsts.data.XErr] || 'Xbox authorization failed (HTTP ' + xsts.status + ')');

  const mc = await json('https://api.minecraftservices.com/authentication/login_with_xbox', {
    identityToken: `XBL3.0 x=${uhs};${xsts.data.Token}`,
  });
  if (mc.status === 403) {
    throw new Error('Microsoft has not approved your Azure app for Minecraft yet (HTTP 403). Request access with the form in the guide, then try again.');
  }
  if (mc.status !== 200) throw new Error('Minecraft login failed (HTTP ' + mc.status + ')');

  const profile = await json('https://api.minecraftservices.com/minecraft/profile', null, { Authorization: 'Bearer ' + mc.data.access_token });
  if (profile.status === 404) throw new Error("This Microsoft account doesn't own Minecraft Java Edition.");
  if (profile.status !== 200) throw new Error('Could not read your Minecraft profile (HTTP ' + profile.status + ')');

  return {
    type: 'microsoft',
    username: profile.data.name,
    uuid: profile.data.id,
    accessToken: mc.data.access_token,
    expiresAt: Date.now() + (mc.data.expires_in || 86400) * 1000 - 60000,
    userType: 'msa',
    skins: profile.data.skins || [],
  };
}

/** Sign-in after the login window returned a code. */
async function signInWithCode(clientId, code, verifier) {
  const ms = await exchangeCode(clientId, code, verifier);
  const account = await minecraftFromMicrosoft(ms.access_token);
  account.refreshToken = ms.refresh_token;
  return account;
}

/** Device-code sign-in (fallback). onCode({userCode, url}) is called so the UI can show the code. */
async function signIn(clientId, onCode, isCancelled) {
  const device = await startDeviceCode(clientId);
  onCode({ userCode: device.userCode, url: device.url });
  const ms = await pollDeviceCode(clientId, device, isCancelled);
  const account = await minecraftFromMicrosoft(ms.access_token);
  account.refreshToken = ms.refresh_token;
  return account;
}

/** Returns a fresh account (re-runs the chain from the refresh token when the Minecraft token expired). */
async function ensureFresh(clientId, account) {
  if (account.expiresAt && Date.now() < account.expiresAt) return account;
  const ms = await refreshMicrosoft(clientId, account.refreshToken);
  const fresh = await minecraftFromMicrosoft(ms.access_token);
  fresh.refreshToken = ms.refresh_token || account.refreshToken;
  return fresh;
}

module.exports = { REDIRECT, authorizeRequest, codeFromRedirect, signInWithCode, signIn, ensureFresh };
