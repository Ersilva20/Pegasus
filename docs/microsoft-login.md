# Microsoft login for Pegasus

Signing in: click your name (top right) → **SIGN IN WITH MICROSOFT** → a Microsoft window
opens → type your **email**, then your **password** (or approve it in the Authenticator app / 2FA) → done.
The badge turns green ("MICROSOFT") and PLAY uses your real account and skin on servers.

Behind that window, every launcher that isn't the official one needs its **own app registration** with Microsoft,
plus Microsoft's approval to use the Minecraft API. Every third-party launcher has one. It's free and you
only do it once.

## 1. Register the app (about 5 minutes)

1. Go to <https://portal.azure.com> and sign in with your Microsoft account
   (Microsoft may ask you to create a free Azure account first).
2. Search for **"App registrations"** → **New registration**.
   - **Name:** `Pegasus`
   - **Supported account types:** *Personal Microsoft accounts only*
   - **Redirect URI:** choose **Public client/native (mobile & desktop)** and type
     `https://login.microsoftonline.com/common/oauth2/nativeclient`
   - Click **Register**.
3. In the new app, open **Authentication**:
   - Check that the `.../nativeclient` URI is listed under *Mobile and desktop applications*
     (if you skipped it above: **Add a platform** → **Mobile and desktop applications** → tick it).
   - Under *Advanced settings* set **Allow public client flows** to **Yes** (needed only for the
     "sign in with a code" fallback) → **Save**.
4. On the **Overview** page, copy the **Application (client) ID** (looks like `1a2b3c4d-…`).

## 2. Paste it in Pegasus

Click your name → the account window shows **ONE-TIME SETUP** → paste → **SAVE**.
(It's also in Settings → *Advanced* → **Azure Client ID**.)

## 3. Ask Microsoft for Minecraft access

New apps get `HTTP 403` from the Minecraft login until Microsoft approves them.
Request access with the review form: <https://aka.ms/mce-reviewappid>
(enter your Client ID and describe the launcher). Approval can take from a few days to a few weeks.

You can already test steps 1–2 before approval: the email/password window works and Xbox signs you in, then
Pegasus stops with a clear message saying the app isn't approved yet.

## Security notes

- You type your password on Microsoft's own page; Pegasus never sees it. The window uses a fresh, empty
  browser session each time and keeps no cookies.
- Pegasus keeps only the login token, encrypted with Windows' protection (DPAPI) in `%APPDATA%\.pegasus\account.dat`.
- Never use another launcher's Client ID; that breaks Microsoft's terms.
