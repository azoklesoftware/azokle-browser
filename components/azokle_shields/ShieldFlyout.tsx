/**
 * Azokle Browser - Shared Desktop UI
 * Azokle Shield Flyout Component (React 19 Mount Bridge)
 */

import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { ShieldsPanel } from './ShieldsPanel.tsx';
import type { BrowserStore } from '../../state/browserStore.ts';
import type { TabStore } from '../../state/tabStore.ts';

export class ShieldFlyoutComponent {
  private container: HTMLElement;
  private browserStore: BrowserStore;
  private tabStore: TabStore;
  private reactRoot: Root | null = null;

  constructor(container: HTMLElement, browserStore: BrowserStore, tabStore: TabStore) {
    this.container = container;
    this.browserStore = browserStore;
    this.tabStore = tabStore;
    this.reactRoot = createRoot(this.container);

    this.browserStore.subscribe(() => this.render());
    this.tabStore.subscribe(() => this.render());
    this.render();
  }

  private render(): void {
    const isOpen = this.browserStore.isShieldOpen();
    if (!isOpen) {
      this.container.style.display = 'none';
      return;
    }

    const activeTab = this.tabStore.getActiveTab();
    let currentHost = 'this site';
    if (activeTab?.securityHost) {
      currentHost = activeTab.securityHost;
    } else if (activeTab?.url && activeTab.url.startsWith('http')) {
      try {
        currentHost = new URL(activeTab.url).hostname;
      } catch {
        currentHost = activeTab.url;
      }
    } else if (activeTab?.url === 'azokle://newtab') {
      currentHost = 'azokle://newtab';
    }

    this.container.style.display = 'block';
    this.container.className = 'flyout-overlay shield-flyout-container';

    if (this.reactRoot) {
      this.reactRoot.render(
        <ShieldsPanel
          currentHost={currentHost}
          favicon={activeTab?.favicon}
          isOpen={isOpen}
          onClose={() => this.browserStore.setShieldOpen(false)}
          onReload={() => {
            this.browserStore.setShieldOpen(false);
            this.tabStore.reload();
          }}
        />
      );
    }
  }
}
