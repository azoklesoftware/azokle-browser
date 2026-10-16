/**
 * Azokle Browser - Shared Desktop UI
 * Main Application Entrypoint
 */

import { getDesktopBridge } from './bridge/bridge.ts';
import { TabStore } from './state/tabStore.ts';
import { BrowserStore } from './state/browserStore.ts';
import { TabStripComponent } from './components/tabstrip/TabStrip.ts';
import { NavigationToolbarComponent } from './components/toolbar/NavigationToolbar.ts';
import { BookmarksBarComponent } from './components/bookmarks/BookmarksBar.ts';
import { ShieldFlyoutComponent } from './components/shield/ShieldFlyout.tsx';
import { TorCircuitModalComponent } from './components/tor/TorCircuitModal.ts';
import { BrowserMenuComponent } from './components/menu/BrowserMenu.ts';

export function initializeDesktopUI(): void {
  console.log('[AzokleDesktopUI] Initializing Azokle Browser Shell...');

  const bridge = getDesktopBridge();
  const tabStore = new TabStore(bridge);
  const browserStore = new BrowserStore(bridge);

  // Mount DOM elements
  const tabstripRoot = document.getElementById('tabstrip-root');
  if (tabstripRoot) new TabStripComponent(tabstripRoot, tabStore);

  const toolbarRoot = document.getElementById('toolbar-root');
  if (toolbarRoot) new NavigationToolbarComponent(toolbarRoot, tabStore, browserStore);

  const bookmarksRoot = document.getElementById('bookmarks-root');
  if (bookmarksRoot) new BookmarksBarComponent(bookmarksRoot, browserStore, tabStore);

  const shieldRoot = document.getElementById('shield-flyout-root');
  if (shieldRoot) new ShieldFlyoutComponent(shieldRoot, browserStore, tabStore);

  const torRoot = document.getElementById('tor-modal-root');
  if (torRoot) new TorCircuitModalComponent(torRoot, browserStore, tabStore);

  const menuRoot = document.getElementById('menu-root');
  if (menuRoot) new BrowserMenuComponent(menuRoot, browserStore, tabStore);

  // Initialize first tab if none exist (ensures tabstrip immediately renders active tab)
  if (tabStore.getTabs().length === 0) {
    tabStore.openTab('azokle://newtab');
  }

  // Sync active tab URL to the in-browser viewport iframe (used during web preview / development)
  const viewportIframe = document.getElementById('web-viewport') as HTMLIFrameElement;
  if (viewportIframe) {
    tabStore.subscribe(() => {
      const activeTab = tabStore.getActiveTab();
      if (activeTab && activeTab.url) {
        if (activeTab.url === 'azokle://newtab' || activeTab.url === 'about:blank') {
          viewportIframe.src = './pages/newtab/index.html';
        } else if (activeTab.url === 'azokle://settings') {
          viewportIframe.src = './pages/settings/index.html';
        } else if (activeTab.url.startsWith('http://') || activeTab.url.startsWith('https://')) {
          try {
            viewportIframe.src = activeTab.url;
          } catch {
            // Content-Security-Policy or X-Frame-Options in local iframe
          }
        }
      }
    });
  }

  // Global click outside to dismiss flyouts
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement;
    if (
      !target.closest('#shield-flyout-root') &&
      !target.closest('#toolbar-shield-btn') &&
      browserStore.isShieldOpen()
    ) {
      browserStore.setShieldOpen(false);
    }

    if (
      !target.closest('#tor-modal-root') &&
      !target.closest('#toolbar-tor-btn') &&
      browserStore.isTorOpen()
    ) {
      browserStore.setTorOpen(false);
    }

    if (
      !target.closest('#menu-root') &&
      !target.closest('#toolbar-menu-btn') &&
      browserStore.isBrowserMenuOpen()
    ) {
      browserStore.setMenuOpen(false);
    }
  });

  console.log('[AzokleDesktopUI] Shared TypeScript UI ready.');
}

// Auto-initialize if running in browser DOM
if (typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeDesktopUI);
  } else {
    initializeDesktopUI();
  }
}
