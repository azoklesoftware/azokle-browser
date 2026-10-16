/**
 * Azokle Browser - Shared Desktop UI
 * Reactive Tab Store
 */

import type { IDesktopBridge, TabInfo, TabGroupInfo } from '../bridge/types.ts';

export type TabStoreListener = () => void;

export class TabStore {
  private bridge: IDesktopBridge;
  private tabs: TabInfo[] = [];
  private activeTabId: number = -1;
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
    return this.bridge.createTab(url, isPrivate, isTor);
  }

  async closeTab(tabId: number): Promise<void> {
    return this.bridge.closeTab(tabId);
  }

  async selectTab(tabId: number): Promise<void> {
    this.activeTabId = tabId;
    this.notify();
    return this.bridge.selectTab(tabId);
  }

  async navigate(url: string): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      return this.bridge.navigate(url, active.id);
    }
  }

  async reload(): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      return this.bridge.reload(active.id);
    }
  }

  async goBack(): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      return this.bridge.goBack(active.id);
    }
  }

  async goForward(): Promise<void> {
    const active = this.getActiveTab();
    if (active) {
      return this.bridge.goForward(active.id);
    }
  }

  getBridge(): IDesktopBridge {
    return this.bridge;
  }
}
