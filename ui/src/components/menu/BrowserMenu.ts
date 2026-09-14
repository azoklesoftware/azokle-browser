/**
 * Azokle Browser - Shared Desktop UI
 * Browser Dropdown Menu Component (Brave-Grade Architecture)
 */

import type { BrowserStore } from '../../state/browserStore.ts';
import type { TabStore } from '../../state/tabStore.ts';

export class BrowserMenuComponent {
  private container: HTMLElement;
  private browserStore: BrowserStore;
  private tabStore: TabStore;
  private zoomLevel: number = 100;

  constructor(container: HTMLElement, browserStore: BrowserStore, tabStore: TabStore) {
    this.container = container;
    this.browserStore = browserStore;
    this.tabStore = tabStore;
    this.browserStore.subscribe(() => this.render());
    this.render();
  }

  private render(): void {
    const isOpen = this.browserStore.isBrowserMenuOpen();
    if (!isOpen) {
      this.container.style.display = 'none';
      return;
    }

    this.container.style.display = 'block';
    this.container.className = 'flyout-overlay browser-menu-container';

    this.container.innerHTML = `
      <!-- Zoom & Fullscreen Row -->
      <div class="menu-zoom-row">
        <span class="menu-zoom-label">Zoom</span>
        <div class="menu-zoom-controls">
          <button class="zoom-btn" id="zoom-dec" title="Zoom Out (Ctrl -)">−</button>
          <span class="zoom-val" id="zoom-val">${this.zoomLevel}%</span>
          <button class="zoom-btn" id="zoom-inc" title="Zoom In (Ctrl +)">＋</button>
          <button class="zoom-btn fullscreen-btn" id="zoom-full" title="Fullscreen (F11)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
          </button>
        </div>
      </div>

      <div class="menu-divider"></div>

      <!-- Section: Windows & Tabs -->
      <div class="menu-item" id="menu-new-tab">
        <span class="menu-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </span>
        <span class="menu-label">New Tab</span>
        <span class="menu-shortcut">Ctrl+T</span>
      </div>
      <div class="menu-item" id="menu-new-window">
        <span class="menu-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/></svg>
        </span>
        <span class="menu-label">New Window</span>
        <span class="menu-shortcut">Ctrl+N</span>
      </div>
      <div class="menu-item" id="menu-new-tor">
        <span class="menu-icon">🧅</span>
        <span class="menu-label">New Private Tor Tab</span>
        <span class="menu-shortcut">Ctrl+Shift+N</span>
      </div>

      <div class="menu-divider"></div>

      <!-- Section: Privacy & Shields -->
      <div class="menu-item" id="menu-shields">
        <span class="menu-icon">🛡️</span>
        <span class="menu-label">Azokle Shields</span>
        <span class="menu-shortcut">Ctrl+Shift+S</span>
      </div>
      <div class="menu-item" id="menu-tor-circuit">
        <span class="menu-icon">🌐</span>
        <span class="menu-label">Tor Circuit Status</span>
        <span class="menu-shortcut">Alt+Shift+T</span>
      </div>

      <div class="menu-divider"></div>

      <!-- Section: Data & Browsing -->
      <div class="menu-item" id="menu-bookmarks">
        <span class="menu-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
        </span>
        <span class="menu-label">Bookmarks</span>
        <span class="menu-shortcut">Ctrl+Shift+O</span>
      </div>
      <div class="menu-item" id="menu-history">
        <span class="menu-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </span>
        <span class="menu-label">History</span>
        <span class="menu-shortcut">Ctrl+H</span>
      </div>
      <div class="menu-item" id="menu-downloads">
        <span class="menu-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        </span>
        <span class="menu-label">Downloads</span>
        <span class="menu-shortcut">Ctrl+J</span>
      </div>

      <div class="menu-divider"></div>

      <!-- Section: Settings & Info -->
      <div class="menu-item" id="menu-settings">
        <span class="menu-icon">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        </span>
        <span class="menu-label">Settings</span>
        <span class="menu-shortcut">Ctrl+,</span>
      </div>
      <div class="menu-item" id="menu-about">
        <span class="menu-icon">ℹ️</span>
        <span class="menu-label">About Azokle Browser</span>
      </div>
    `;

    // Hook zoom buttons
    this.container.querySelector('#zoom-dec')?.addEventListener('click', (e) => {
      e.stopPropagation();
      if (this.zoomLevel > 50) this.zoomLevel -= 10;
      const valEl = this.container.querySelector('#zoom-val');
      if (valEl) valEl.textContent = `${this.zoomLevel}%`;
    });

    this.container.querySelector('#zoom-inc')?.addEventListener('click', (e) => {
      e.stopPropagation();
      if (this.zoomLevel < 200) this.zoomLevel += 10;
      const valEl = this.container.querySelector('#zoom-val');
      if (valEl) valEl.textContent = `${this.zoomLevel}%`;
    });

    this.container.querySelector('#zoom-full')?.addEventListener('click', (e) => {
      e.stopPropagation();
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen?.();
      } else {
        document.exitFullscreen?.();
      }
      this.browserStore.setMenuOpen(false);
    });

    // Helper to close menu and trigger action
    const closeAndRun = (fn: () => void) => {
      this.browserStore.setMenuOpen(false);
      fn();
    };

    this.container.querySelector('#menu-new-tab')?.addEventListener('click', () => closeAndRun(() => this.tabStore.openTab('azokle://newtab')));
    this.container.querySelector('#menu-new-window')?.addEventListener('click', () => closeAndRun(() => this.tabStore.openTab('azokle://newtab')));
    this.container.querySelector('#menu-new-tor')?.addEventListener('click', () => closeAndRun(() => this.tabStore.openTab('https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion', true, true)));
    this.container.querySelector('#menu-shields')?.addEventListener('click', () => closeAndRun(() => this.browserStore.setShieldOpen(true)));
    this.container.querySelector('#menu-tor-circuit')?.addEventListener('click', () => closeAndRun(() => this.browserStore.setTorOpen(true)));
    this.container.querySelector('#menu-bookmarks')?.addEventListener('click', () => closeAndRun(() => this.tabStore.navigate('azokle://bookmarks')));
    this.container.querySelector('#menu-history')?.addEventListener('click', () => closeAndRun(() => this.tabStore.navigate('azokle://history')));
    this.container.querySelector('#menu-downloads')?.addEventListener('click', () => closeAndRun(() => this.tabStore.navigate('azokle://downloads')));
    this.container.querySelector('#menu-settings')?.addEventListener('click', () => closeAndRun(() => this.tabStore.navigate('azokle://settings')));
    this.container.querySelector('#menu-about')?.addEventListener('click', () => closeAndRun(() => {
      alert('Azokle Browser v2.0.0\nBuilt with C++20, Rust, GeckoEngine, and React Desktop UI\nZero telemetry.');
    }));
  }
}
