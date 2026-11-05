'use strict';
// Command-line front end for the launcher engine (useful for testing without the UI).
//   node src/cli.js install          download/verify everything
//   node src/cli.js launch [name]    install if needed and start the game
const settingsStore = require('./core/settings');
const { prepare, launch } = require('./core/launch');

function bar(text, p) {
  const pct = Math.round((p || 0) * 100);
  process.stdout.write(`\r${text.padEnd(48).slice(0, 48)} ${String(pct).padStart(3)}%`);
}

(async () => {
  const cmd = process.argv[2] || 'install';
  const settings = settingsStore.load();
  if (process.argv[3]) settings.username = process.argv[3];
  try {
    if (cmd === 'install') {
      const p = await prepare(settings, bar);
      console.log('\nReady. Java:', p.java, '\nGame dir:', p.gameDir);
    } else if (cmd === 'launch') {
      await launch(settings, {
        progress: bar,
        onLog: (l) => console.log(l),
        onExit: (c) => console.log('\nGame exited with code', c),
      });
    }
  } catch (e) {
    console.error('\nError:', e.message);
    process.exit(1);
  }
})();
