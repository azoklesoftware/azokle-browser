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

    // Left Navigation Controls (Back, Forward, Reload, Home)
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
      'Reload this page (Ctrl+R)',
      `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>`,
      () => this.tabStore.reload()
    );

    const btnHome = this.createSvgButton(
      'nav-btn-home',
      'Open homepage (Alt+Home)',
      `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`,
      () => this.tabStore.navigate('azokle://newtab')
    );

    leftNav.appendChild(btnBack);
    leftNav.appendChild(btnForward);
    leftNav.appendChild(btnReload);
    leftNav.appendChild(btnHome);

    // Center Omnibar Slot
    const omnibarSlot = document.createElement('div');
    omnibarSlot.className = 'omnibar-slot';
    new OmnibarComponent(omnibarSlot, this.tabStore, this.browserStore);

    // Right Action Controls
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

    // Tor Circuit Button
    const torBtn = document.createElement('button');
    torBtn.id = 'toolbar-tor-btn';
    torBtn.className = 'icon-btn tor-btn';
    torBtn.title = 'Tor Onion Routing Circuit';
    torBtn.setAttribute('aria-label', 'Tor Routing');
    torBtn.onclick = () => {
      this.browserStore.setTorOpen(!this.browserStore.isTorOpen());
      this.browserStore.setShieldOpen(false);
      this.browserStore.setMenuOpen(false);
    };

    // App Menu Button (3 Dots)
    const menuBtn = document.createElement('button');
    menuBtn.id = 'toolbar-menu-btn';
    menuBtn.className = 'icon-btn menu-btn';
    menuBtn.title = 'Customize and control Azokle';
    menuBtn.setAttribute('aria-label', 'Main Menu');
    menuBtn.innerHTML = `
      <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
        <circle cx="12" cy="5" r="2"/>
        <circle cx="12" cy="12" r="2"/>
        <circle cx="12" cy="19" r="2"/>
      </svg>
    `;
    menuBtn.onclick = () => {
      this.browserStore.setMenuOpen(!this.browserStore.isBrowserMenuOpen());
      this.browserStore.setShieldOpen(false);
      this.browserStore.setTorOpen(false);
    };

    rightNav.appendChild(shieldBtn);
    rightNav.appendChild(torBtn);
    rightNav.appendChild(menuBtn);

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
