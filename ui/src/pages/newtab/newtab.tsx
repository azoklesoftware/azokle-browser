/**
 * Azokle Browser - Shared Desktop UI
 * New Tab Page React 19 Mount Script
 */

import React from 'react';
import { createRoot } from 'react-dom/client';
import { NewTabPage } from './NewTabPage.tsx';

const rootEl = document.getElementById('newtab-root');
if (rootEl) {
  const root = createRoot(rootEl);
  root.render(<NewTabPage />);
}
