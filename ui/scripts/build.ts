/**
 * Cross-Platform Build Script for Azokle Desktop UI (Bun)
 */

import { build } from 'bun';
import { existsSync, mkdirSync, copyFileSync, readFileSync, writeFileSync } from 'fs';
import { resolve, join } from 'path';

const ROOT_DIR = resolve(import.meta.dir, '..');
const DIST_DIR = join(ROOT_DIR, 'dist');

console.log('[AzokleBuild] Starting Bun desktop UI bundle build...');

if (!existsSync(DIST_DIR)) {
  mkdirSync(DIST_DIR, { recursive: true });
}

// 1. Build main application JS bundle
const mainBuild = await build({
  entrypoints: [join(ROOT_DIR, 'src', 'index.ts')],
  outdir: DIST_DIR,
  minify: true,
  target: 'browser',
  naming: '[name].[ext]'
});

if (!mainBuild.success) {
  console.error('[AzokleBuild] Main build failed:', mainBuild.logs);
  process.exit(1);
}

// 2. Build internal pages (newtab, settings)
const newtabDir = join(DIST_DIR, 'pages', 'newtab');
const settingsDir = join(DIST_DIR, 'pages', 'settings');
mkdirSync(newtabDir, { recursive: true });
mkdirSync(settingsDir, { recursive: true });

await build({
  entrypoints: [join(ROOT_DIR, 'src', 'pages', 'newtab', 'newtab.tsx')],
  outdir: newtabDir,
  minify: true,
  target: 'browser',
  naming: '[name].[ext]'
});

await build({
  entrypoints: [join(ROOT_DIR, 'src', 'pages', 'settings', 'settings.ts')],
  outdir: settingsDir,
  minify: true,
  target: 'browser',
  naming: '[name].[ext]'
});

const WORKSPACE_DIR = resolve(ROOT_DIR, '..');

// 3. Concatenate all CSS into dist/bundle.css
const cssFiles = [
  join(WORKSPACE_DIR, 'third_party', 'azokle_ui', 'theme.css'),
  join(WORKSPACE_DIR, 'third_party', 'azokle_ui', 'global.css'),
  join(ROOT_DIR, 'src', 'styles', 'theme.css'),
  join(ROOT_DIR, 'src', 'styles', 'global.css'),
  join(ROOT_DIR, 'src', 'components', 'tabstrip', 'tabstrip.css'),
  join(ROOT_DIR, 'src', 'components', 'omnibar', 'omnibar.css'),
  join(ROOT_DIR, 'src', 'components', 'toolbar', 'toolbar.css'),
  join(ROOT_DIR, 'src', 'components', 'shield', 'shield.css'),
  join(ROOT_DIR, 'src', 'components', 'shield', 'shields-panel.css'),
  join(ROOT_DIR, 'src', 'components', 'tor', 'tor.css'),
  join(ROOT_DIR, 'src', 'components', 'bookmarks', 'bookmarks.css'),
  join(ROOT_DIR, 'src', 'components', 'menu', 'menu.css')
];

let concatenatedCss = '/* Azokle Desktop UI - Compiled Bundle */\n';
const seenFiles = new Set<string>();
for (const file of cssFiles) {
  if (existsSync(file) && !seenFiles.has(file)) {
    seenFiles.add(file);
    concatenatedCss += `\n/* --- ${file.split('/').slice(-2).join('/')} --- */\n`;
    concatenatedCss += readFileSync(file, 'utf-8');
  }
}
writeFileSync(join(DIST_DIR, 'bundle.css'), concatenatedCss, 'utf-8');

// 4. Generate dist/index.html with bundled assets
const distIndexHtml = `<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src https: http: data:; object-src 'none'; base-uri 'none';">
  <title>Azokle Browser</title>
  <link rel="stylesheet" href="./bundle.css">
</head>
<body>
  <div id="app-container">
    <header class="browser-chrome-header">
      <div id="tabstrip-root"></div>
      <div id="toolbar-root"></div>
      <div id="bookmarks-root"></div>
    </header>

    <main class="viewport-container" id="viewport-root">
      <iframe id="web-viewport" class="viewport-iframe" src="./pages/newtab/index.html"></iframe>
    </main>

    <!-- Floating Overlays -->
    <div id="shield-flyout-root"></div>
    <div id="tor-modal-root"></div>
    <div id="menu-root"></div>
  </div>

  <script type="module" src="./index.js"></script>
</body>
</html>`;

writeFileSync(join(DIST_DIR, 'index.html'), distIndexHtml, 'utf-8');

// Copy HTML pages and assets into dist
copyFileSync(
  join(ROOT_DIR, 'src', 'pages', 'newtab', 'newtab.html'),
  join(newtabDir, 'index.html')
);
copyFileSync(
  join(ROOT_DIR, 'src', 'pages', 'newtab', 'newtab.css'),
  join(newtabDir, 'newtab.css')
);
copyFileSync(
  join(ROOT_DIR, 'src', 'pages', 'settings', 'settings.html'),
  join(settingsDir, 'index.html')
);

console.log('[AzokleBuild] Build finished successfully into ui/dist!');
