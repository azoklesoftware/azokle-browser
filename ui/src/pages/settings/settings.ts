/**
 * Azokle Browser - Shared Desktop UI
 * Settings Page Script
 */

import { getDesktopBridge } from '../../bridge/bridge.ts';

const bridge = getDesktopBridge();

const shieldSelect = document.getElementById('shield-level') as HTMLSelectElement;
const searchSelect = document.getElementById('search-engine-select') as HTMLSelectElement;
const torSelect = document.getElementById('tor-proxy-mode') as HTMLSelectElement;

if (shieldSelect) {
  shieldSelect.addEventListener('change', async () => {
    if (shieldSelect.value === 'off') {
      await bridge.toggleShield();
    }
  });
}

if (torSelect) {
  torSelect.addEventListener('change', async () => {
    await bridge.toggleTor();
  });
}
