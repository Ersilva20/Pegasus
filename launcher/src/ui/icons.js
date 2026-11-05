'use strict';
// Pegasus pixel icon set. Each icon is a grid: '.' = transparent, letters = palette colors.
// Rendered once to PNG data URLs and shown with image-rendering: pixelated.
const PALETTE = {
  o: '#0d0910', // outline
  W: '#fff6e0', // white/cream
  C: '#f3e9d2', // cream wall
  R: '#d6403a', r: '#9b2a26', // red
  G: '#3ccb4f', g: '#23913a', // green
  B: '#8a5a3b', b: '#6b432b', // dirt
  Y: '#f2c14e', y: '#c9922e', // yellow
  S: '#c9cfdc', s: '#8e95a8', // silver
  K: '#15101b', // screen black
  T: '#d9823b', t: '#a85a22', // copper
  L: '#3fb5f0', l: '#1f6fa8', // blue
  P: '#a87be8', p: '#6c46b0', // purple
};

const ICONS = {
  home: [
    '.....oo.....',
    '....oRRo....',
    '...oRRRRo...',
    '..oRRRRRRo..',
    '.oRRRRRRRRo.',
    'oRrrrrrrrrRo',
    'oooooooooooo',
    '.oCCCCCCCCo.',
    '.oCooCCLLCo.',
    '.oCooCCLLCo.',
    '.oCooCCCCCo.',
    '.oooooooooo.',
  ],
  mods: [
    'oooooooooooo',
    'oGGGGGGGGGGo',
    'oGgGGGgGGgGo',
    'ogBgBgggBgBo',
    'oBBBBBBBBBBo',
    'oBbBBBBbBBBo',
    'oBBBBbBBBBBo',
    'oBBBBBBBBbBo',
    'oBbBBBBBBBBo',
    'oBBBBBbBBBBo',
    'oBBBBBBBBBBo',
    'oooooooooooo',
  ],
  settings: [
    '....oooo....',
    '.oo.oSSo.oo.',
    '.oSooSSooSo.',
    '..oSSSSSSo..',
    'ooSSSssSSSoo',
    'oSSSs..sSSSo',
    'oSSSs..sSSSo',
    'ooSSSssSSSoo',
    '..oSSSSSSo..',
    '.oSooSSooSo.',
    '.oo.oSSo.oo.',
    '....oooo....',
  ],
  console: [
    'oooooooooooo',
    'oTTTTTTTWTWo',
    'oooooooooooo',
    'oKKKKKKKKKKo',
    'oKGKKKKKKKKo',
    'oKKGKKKKKKKo',
    'oKKKGKKKKKKo',
    'oKKGKKKKKKKo',
    'oKGKKKGGGKKo',
    'oKKKKKKKKKKo',
    'oKKKKKKKKKKo',
    'oooooooooooo',
  ],
  folder: [
    '............',
    '.oooo.......',
    'oYYYYo......',
    'oYYYYooooooo',
    'oyyyyyyyyyyo',
    'oYYYYYYYYYYo',
    'oYYYYYYYYYYo',
    'oYYYYYYYYYYo',
    'oYYYYYYYYYYo',
    'oyyyyyyyyyyo',
    'oooooooooooo',
    '............',
  ],
  chip: [
    '............',
    '.o.o.o.o.o..',
    'oooooooooooo',
    'oGGGGGGGGGGo',
    'oGKKGKKGKKGo',
    'oGKKGKKGKKGo',
    'oGKKGKKGKKGo',
    'oggggggggggo',
    'oooooooooooo',
    '.o.o.o.o.o..',
    '............',
    '............',
  ],
  play: [
    '.oo.......',
    '.oWoo.....',
    '.oWWWoo...',
    '.oWWWWWoo.',
    '.oWWWWWWWo',
    '.oWWWWWWWo',
    '.oWWWWWoo.',
    '.oWWWoo...',
    '.oWoo.....',
    '.oo.......',
  ],
  check: [
    '........',
    '......oo',
    '.....oWo',
    'oo..oWo.',
    'oWooWo..',
    '.oWWo...',
    '..oo....',
    '........',
  ],
  cross: [
    'oo....oo',
    'oWo..oWo',
    '.oWooWo.',
    '..oWWo..',
    '..oWWo..',
    '.oWooWo.',
    'oWo..oWo',
    'oo....oo',
  ],
  java: [
    '...o.o......',
    '..o.o.......',
    '...o.o......',
    'oooooooooo..',
    'oCCCCCCCCoo.',
    'oCTTTTTTCoCo',
    'oCTTTTTTCoCo',
    'oCCCCCCCCoo.',
    '.oCCCCCCo...',
    '..oooooo....',
    'oooooooooooo',
    '............',
  ],
  server: [
    'oooooooooooo',
    'oSSSSSSSSSSo',
    'oSKKKKKSGSGo',
    'oSSSSSSSSSSo',
    'oooooooooooo',
    'oSSSSSSSSSSo',
    'oSKKKKKSGSRo',
    'oSSSSSSSSSSo',
    'oooooooooooo',
    '.....oo.....',
    '..oooooooo..',
    '............',
  ],
  heart: [
    '.oo...oo.',
    'oRRo.oRRo',
    'oRWRoRRRo',
    'oRRRRRRRo',
    '.oRRRRRo.',
    '..oRRRo..',
    '...oRo...',
    '....o....',
  ],
  steve: [
    'bbbbbbbb',
    'bbbbbbbb',
    'bCCCCCCb',
    'CCCCCCCC',
    'CWpCCpWC',
    'CCCbbCCC',
    'CCbrrbCC',
    'CCCCCCCC',
  ],
};

// Steve's skin tone uses its own colors.
const STEVE = { b: '#2b1e10', C: '#b78a6d', W: '#ffffff', p: '#523d89', r: '#6a4030' };

function iconUrl(name) {
  const rows = ICONS[name];
  if (!rows) return '';
  const pal = name === 'steve' ? STEVE : PALETTE;
  const c = document.createElement('canvas');
  c.width = rows[0].length;
  c.height = rows.length;
  const g = c.getContext('2d');
  rows.forEach((row, y) => {
    [...row].forEach((ch, x) => {
      if (ch === '.') return;
      g.fillStyle = pal[ch] || '#ff00ff';
      g.fillRect(x, y, 1, 1);
    });
  });
  return c.toDataURL();
}

/** Replaces every <i data-icon="name"> with a crisp pixel image. */
function applyIcons(root) {
  (root || document).querySelectorAll('i[data-icon]').forEach((el) => {
    const img = document.createElement('img');
    img.src = iconUrl(el.dataset.icon);
    img.className = 'pxi ' + (el.className || '');
    img.alt = '';
    el.replaceWith(img);
  });
}

window.PixelIcons = { iconUrl, applyIcons };
