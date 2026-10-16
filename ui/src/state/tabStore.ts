/**
 * Azokle Browser - Shared Desktop UI
 * Reactive Tab Store
 */

import type { IDesktopBridge, TabInfo, TabGroupInfo } from '../bridge/types.ts';

export type TabStoreListener = () => void;

export class TabStore {
  private bridge: IDesktopBridge;
  private tabs: TabInfo[] = [{
    id: 1,
    title: 'New Tab',
    url: 'azokle://newtab',
    isPrivate: false,
    isTor: false,
    isLoading: false,
    progress: 1.0,
    securityLevel: 'SECURE'
  }];
  private activeTabId: number = 1;
  private groups: Map<string, TabGroupInfo> = new Map();
  private listeners: Set<TabStoreListener> = new Set();

  constructor(bridge: IDesktopBridge) {
    this.bridge = bridge;
    this.initEventListeners();
  }

  private initEventListeners(): void {
    this.bridge.on('tab-created', (tab: TabInfo) => {
      const existing = this.tabs.findIndex(t => t.id === tab.id);
      if (existing >= 0) {
        this.tabs[existing] = tab;
      } else {
        this.tabs.push(tab);
      }
      this.activeTabId = tab.id;
      this.notify();
    });

    this.bridge.on('tab-closed', ({ tabId }) => {
      this.tabs = this.tabs.filter(t => t.id !== tabId);
      this.notify();
    });

    this.bridge.on('tab-activated', ({ tabId }) => {
      this.activeTabId = tabId;
      this.notify();
    });

    this.bridge.on('url-changed', ({ tabId, url }) => {
      const tab = this.tabs.find(t => t.id === tabId);
      if (tab) {
        tab.url = url;
        this.notify();
      }
    });

    this.bridge.on('title-changed', ({ tabId, title }) => {
      const tab = this.tabs.find(t => t.id === tabId);
      if (tab) {
        tab.title = title;
        this.notify();
      }
    });

    this.bridge.on('progress-changed', ({ tabId, progress }) => {
      const tab = this.tabs.find(t => t.id === tabId);
      if (tab) {
        tab.progress = progress;
        this.notify();
      }
    });

    this.bridge.on('loading-state-changed', ({ tabId, isLoading }) => {
      const tab = this.tabs.find(t => t.id === tabId);
      if (tab) {
        tab.isLoading = isLoading;
        this.notify();
      }
    });

    this.bridge.on('security-state-changed', ({ tabId, level, host }) => {
      const tab = this.tabs.find(t => t.id === tabId);
      if (tab) {
        tab.securityLevel = level;
        tab.securityHost = host;
        this.notify();
      }
    });
  }

  subscribe(listener: TabStoreListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private notify(): void {
    for (const listener of this.listeners) {
      listener();
    }
  }

  getTabs(): TabInfo[] {
    return [...this.tabs];
  }

  getActiveTab(): TabInfo | undefined {
    return this.tabs.find(t => t.id === this.activeTabId) ?? this.tabs[0];
  }

  getActiveTabId(): number {
    return this.activeTabId;
  }

  async openTab(url?: string, isPrivate?: boolean, isTor?: boolean): Promise<TabInfo> {
    const tab = await this.bridge.createTab(url, isPrivate, isTor);
    if (tab && tab.id) {
      const existing = this.tabs.findIndex(t => t.id === tab.id);
      if (existing >= 0) {
        this.tabs[existing] = tab;
      } else {
        this.tabs.push(tab);
      }
      this.activeTabId = tab.id;
      this.notify();
    }
    return tab;
  }

  async closeTab(tabId: number): Promise<void> {
    await this.bridge.closeTab(tabId);
    this.tabs = this.tabs.filter(t => t.id !== tabId);
    if (this.tabs.length === 0) {
      this.tabs.push({
        id: Date.now(),
        title: 'New Tab',
        url: 'azokle://newtab',
        isPrivate: false,
        isTor: false,
        isLoading: false,
        progress: 1.0,
        securityLevel: 'SECURE'
      });
    }
    if (!this.tabs.some(t => t.id === this.activeTabId)) {
      this.activeTabId = this.tabs[this.tabs.length - 1].id;
    }
    this.notify();
  }

  async selectTab(tabId: number): Promise<void> {
    this.activeTabId = tabId;
    this.notify();
    return this.bridge.selectTab(tabId);
  }

  async navigate(url: string): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      active.url = url;
      active.isLoading = true;
      active.progress = 0.2;
      try {
        if (url.startsWith('http://') || url.startsWith('https://')) {
          const u = new URL(url);
          active.title = u.hostname;
          active.securityHost = u.hostname;
          active.securityLevel = url.startsWith('https://') ? 'SECURE' : 'LOCAL';
        } else if (url === 'azokle://newtab' || url === 'about:blank') {
          active.title = 'New Tab';
          active.securityLevel = 'SECURE';
        } else if (url === 'azokle://settings') {
          active.title = 'Settings';
          active.securityLevel = 'LOCAL';
        }
      } catch {
        active.title = url;
      }
      this.notify();
      return this.bridge.navigate(url, active.id);
    }
  }

  async reload(): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      const viewport = typeof document !== 'undefined' ? (document.getElementById('web-viewport') as HTMLIFrameElement) : null;
      if (viewport && viewport.contentWindow) {
        try {
          viewport.contentWindow.location.reload();
          return;
        } catch {}
      }
      return this.bridge.reload(active.id);
    }
  }

  async goBack(): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      const viewport = typeof document !== 'undefined' ? (document.getElementById('web-viewport') as HTMLIFrameElement) : null;
      if (viewport && viewport.contentWindow) {
        try {
          viewport.contentWindow.history.back();
          return;
        } catch {}
      }
      return this.bridge.goBack(active.id);
    }
  }

  async goForward(): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      const viewport = typeof document !== 'undefined' ? (document.getElementById('web-viewport') as HTMLIFrameElement) : null;
      if (viewport && viewport.contentWindow) {
        try {
          viewport.contentWindow.history.forward();
          return;
        } catch {}
      }
      return this.bridge.goForward(active.id);
    }
  }

  getBridge(): IDesktopBridge {
    return this.bridge;
  }
}
