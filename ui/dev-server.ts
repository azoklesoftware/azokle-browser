/**
 * Lightweight Local Dev Server for Shared TypeScript Desktop UI
 * Serves http://localhost:3000 with instant hot reloading
 */

import { resolve, join } from 'path';

const PORT = 3000;
const ROOT = resolve(import.meta.dir);

console.log(`[AzokleDevServer] Starting shared UI dev server on http://localhost:${PORT}...`);

Bun.serve({
  hostname: '127.0.0.1',
  port: PORT,
  async fetch(req) {
    // AZK-MED-12: Validate Host header against DNS rebinding
    const hostHeader = req.headers.get('host') || '';
    if (hostHeader !== `localhost:${PORT}` && hostHeader !== `127.0.0.1:${PORT}`) {
      return new Response('Forbidden: Invalid Host Header', { status: 403 });
    }

    const url = new URL(req.url);
    let pathname = url.pathname;

    if (pathname === '/' || pathname === '') {
      pathname = '/index.html';
    }

    const filePath = resolve(join(ROOT, pathname));
    // AZK-MED-12: Enforce strict path containment to prevent directory traversal
    if (!filePath.startsWith(ROOT)) {
      return new Response('Forbidden', { status: 403 });
    }

    const file = Bun.file(filePath);

    if (await file.exists()) {
      return new Response(file);
    }

    // Check in dist if not in root
    const distPath = resolve(join(ROOT, 'dist', pathname));
    if (distPath.startsWith(ROOT)) {
      const distFile = Bun.file(distPath);
      if (await distFile.exists()) {
        return new Response(distFile);
      }
    }

    return new Response('Not Found', { status: 404 });
  }
});

console.log(`[AzokleDevServer] Ready! Open http://localhost:${PORT} to test the Shared TypeScript UI.`);
