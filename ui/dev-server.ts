/**
 * Lightweight Local Dev Server for Shared TypeScript Desktop UI
 * Serves http://localhost:3000 with instant hot reloading
 */

import { resolve, join } from 'path';

const PORT = 3000;
const ROOT = resolve(import.meta.dir);

console.log(`[AzokleDevServer] Starting shared UI dev server on http://localhost:${PORT}...`);

Bun.serve({
  port: PORT,
  async fetch(req) {
    const url = new URL(req.url);
    let pathname = url.pathname;

    if (pathname === '/' || pathname === '') {
      pathname = '/index.html';
    }

    const filePath = join(ROOT, pathname);
    const file = Bun.file(filePath);

    if (await file.exists()) {
      return new Response(file);
    }

    // Check in dist if not in root
    const distPath = join(ROOT, 'dist', pathname);
    const distFile = Bun.file(distPath);
    if (await distFile.exists()) {
      return new Response(distFile);
    }

    return new Response('Not Found', { status: 404 });
  }
});

console.log(`[AzokleDevServer] Ready! Open http://localhost:${PORT} to test the Shared TypeScript UI.`);
