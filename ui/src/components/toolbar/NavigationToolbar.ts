/**
 * Azokle Browser - Shared Desktop UI
 * Navigation Toolbar Component (Brave-Grade Architecture)
 */

import type { TabStore } from '../../state/tabStore.ts';
import type { BrowserStore } from '../../state/browserStore.ts';
import { OmnibarComponent } from '../omnibar/Omnibar.ts';

export class NavigationToolbarComponent {
  private container: HTMLElement;
  private tabStore: TabStore;
  private browserStore: BrowserStore;

  constructor(container: HTMLElement, tabStore: TabStore, browserStore: BrowserStore) {
    this.container = container;
    this.tabStore = tabStore;
    this.browserStore = browserStore;
    this.render();
    this.browserStore.subscribe(() => this.updateState());
    this.tabStore.subscribe(() => this.updateState());
  }

  private render(): void {
    this.container.className = 'toolbar-container';
    this.container.innerHTML = '';

    // Left Navigation Controls: Back, Forward, Refresh, Bookmark
    const leftNav = document.createElement('div');
    leftNav.className = 'nav-buttons-group';

    const btnBack = this.createSvgButton(
      'nav-btn-back',
      'Click to go back, hold to see history (Alt+Left)',
      `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>`,
      () => this.tabStore.goBack()
    );

    const btnForward = this.createSvgButton(
      'nav-btn-forward',
      'Click to go forward, hold to see history (Alt+Right)',
      `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>`,
      () => this.tabStore.goForward()
    );

    const btnReload = this.createSvgButton(
      'nav-btn-reload',
      'Refresh this page (Ctrl+R)',
      `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>`,
      () => this.tabStore.reload()
    );

    const btnBookmark = this.createSvgButton(
      'nav-btn-bookmark',
      'Bookmark this page (Ctrl+D)',
      `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/></svg>`,
      () => {
        const activeTab = this.tabStore.getActiveTab();
        if (activeTab) {
          const isAdded = this.browserStore.toggleBookmark({
            id: `bm-${Date.now()}`,
            title: activeTab.title || 'New Tab',
            url: activeTab.url || 'azokle://newtab',
            favicon: activeTab.favicon
          });
          btnBookmark.classList.toggle('bookmarked', isAdded);
        }
      }
    );

    leftNav.appendChild(btnBack);
    leftNav.appendChild(btnForward);
    leftNav.appendChild(btnReload);
    leftNav.appendChild(btnBookmark);

    // Center Searchbox / Omnibar
    const omnibarSlot = document.createElement('div');
    omnibarSlot.className = 'omnibar-slot';
    new OmnibarComponent(omnibarSlot, this.tabStore, this.browserStore);

    // Right Action Controls: Shield & Settings
    const rightNav = document.createElement('div');
    rightNav.className = 'action-buttons-group';

    // Brave-Style Shield Pill Button
    const shieldBtn = document.createElement('button');
    shieldBtn.id = 'toolbar-shield-btn';
    shieldBtn.className = 'shield-pill-btn';
    shieldBtn.title = 'Azokle Shield Protections';
    shieldBtn.setAttribute('aria-label', 'Azokle Shields');
    shieldBtn.onclick = () => {
      this.browserStore.setShieldOpen(!this.browserStore.isShieldOpen());
      this.browserStore.setTorOpen(false);
      this.browserStore.setMenuOpen(false);
    };

    // Settings / App Menu Button (Gear Icon)
    const settingsBtn = document.createElement('button');
    settingsBtn.id = 'toolbar-menu-btn';
    settingsBtn.className = 'icon-btn settings-btn';
    settingsBtn.title = 'Settings and Browser Menu';
    settingsBtn.setAttribute('aria-label', 'Settings');
    settingsBtn.innerHTML = `
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="3"/>
        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
      </svg>
    `;
    settingsBtn.onclick = () => {
      this.browserStore.setMenuOpen(!this.browserStore.isBrowserMenuOpen());
      this.browserStore.setShieldOpen(false);
      this.browserStore.setTorOpen(false);
    };

    rightNav.appendChild(shieldBtn);
    rightNav.appendChild(settingsBtn);

    this.container.appendChild(leftNav);
    this.container.appendChild(omnibarSlot);
    this.container.appendChild(rightNav);

    this.updateState();
  }

  private updateState(): void {
    // 1. Update Shield Button
    const shieldBtn = this.container.querySelector('#toolbar-shield-btn');
    if (shieldBtn) {
      const metrics = this.browserStore.getShieldMetrics();
      if (metrics.isShieldActive) {
        shieldBtn.className = 'shield-pill-btn active';
        shieldBtn.innerHTML = `
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span class="shield-count">${Number(metrics.totalBlocked) || 0}</span>
        `;
      } else {
        shieldBtn.className = 'shield-pill-btn off';
        shieldBtn.innerHTML = `
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <span class="shield-count">DOWN</span>
        `;
      }
    }

    // 2. Update Tor Button
    const torBtn = this.container.querySelector('#toolbar-tor-btn');
    if (torBtn) {
      const tor = this.browserStore.getTorCircuit();
      torBtn.className = `icon-btn tor-btn ${tor.isActive ? 'active' : ''}`;
      torBtn.innerHTML = `
        <span class="tor-onion-icon">🧅</span>
        ${tor.isActive ? '<span class="tor-active-badge"></span>' : ''}
      `;
    }

    // 3. Update Reload icon to Stop icon if tab is loading
    const activeTab = this.tabStore.getActiveTab();
    const reloadBtn = this.container.querySelector('#nav-btn-reload') as HTMLButtonElement;
    if (reloadBtn && activeTab) {
      if (activeTab.isLoading) {
        reloadBtn.title = 'Stop loading this page (Esc)';
        reloadBtn.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        `;
      } else {
        reloadBtn.title = 'Reload this page (Ctrl+R)';
        reloadBtn.innerHTML = `
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
        `;
      }
    }
  }

  private createSvgButton(id: string, title: string, svgContent: string, onClick: () => void): HTMLButtonElement {
    const btn = document.createElement('button');
    btn.id = id;
    btn.className = 'icon-btn nav-btn';
    btn.title = title;
    btn.setAttribute('aria-label', title);
    btn.innerHTML = svgContent;
    btn.onclick = onClick;
    return btn;
  }
}
