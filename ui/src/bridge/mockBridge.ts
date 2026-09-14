/**
 * Azokle Browser - Shared Desktop UI
 * In-Memory Mock IPC Bridge for Development & Testing
 */

import type {
  IDesktopBridge,
  TabInfo,
  TabGroupInfo,
  ShieldMetrics,
  TorCircuitState,
  BookmarkItem,
  HistoryItem,
  DownloadItem,
  HostEventMap
} from './types.ts';

export class MockDesktopBridge implements IDesktopBridge {
  private tabs: Map<number, TabInfo> = new Map();
  private activeTabId: number = 1;
  private nextTabId: number = 2;
  private groups: Map<string, TabGroupInfo> = new Map();
  private listeners: Map<string, Set<Function>> = new Map();

  private shieldMetrics: ShieldMetrics = {
    totalBlocked: 0,
    adsBlocked: 0,
    trackersBlocked: 0,
    httpsUpgrades: 0,
    isShieldActive: true,
    fingerprintProtection: true,
    cookieBlocking: true
  };

  private torCircuit: TorCircuitState = {
    isActive: false,
    hops: [
      { name: 'GuardRelay-DE', ip: '185.220.101.5', country: 'Germany', isGuard: true },
      { name: 'MiddleRelay-IS', ip: '194.36.144.2', country: 'Iceland' },
      { name: 'ExitRelay-CH', ip: '179.43.149.12', country: 'Switzerland', isExit: true }
    ]
  };

  private bookmarks: BookmarkItem[] = [
    { id: '1', title: 'Azokle Search', url: 'https://search.azokle.com', favicon: '🌐' },
    { id: '2', title: 'GitHub', url: 'https://github.com', favicon: '🐙' },
    { id: '3', title: 'Wikipedia', url: 'https://en.wikipedia.org', favicon: '📚' },
    { id: '4', title: 'Reddit', url: 'https://reddit.com', favicon: '💬' }
  ];

  constructor() {
    // Initial tab
    const initialTab: TabInfo = {
      id: 1,
      title: 'Azokle Search',
      url: 'https://search.azokle.com',
      isPrivate: false,
      isTor: false,
      isLoading: false,
      progress: 1.0,
      securityLevel: 'SECURE',
      securityHost: 'search.azokle.com'
    };
    this.tabs.set(1, initialTab);
  }

  async createTab(url: string = 'azokle://newtab', isPrivate: boolean = false, isTor: boolean = false): Promise<TabInfo> {
    const id = this.nextTabId++;
    const tab: TabInfo = {
      id,
      title: url === 'azokle://newtab' ? 'New Tab' : url,
      url,
      isPrivate,
      isTor,
      isLoading: false,
      progress: 1.0,
      securityLevel: url.startsWith('https://') ? 'SECURE' : 'LOCAL',
      securityHost: url.startsWith('http') ? new URL(url).hostname : undefined
    };

    this.tabs.set(id, tab);
    this.activeTabId = id;
    this.emit('tab-created', tab);
    this.emit('tab-activated', { tabId: id });
    return tab;
  }

  async closeTab(tabId: number): Promise<void> {
    this.tabs.delete(tabId);
    this.emit('tab-closed', { tabId });

    if (this.activeTabId === tabId) {
      const remaining = Array.from(this.tabs.keys());
      if (remaining.length > 0) {
        this.activeTabId = remaining[remaining.length - 1];
        this.emit('tab-activated', { tabId: this.activeTabId });
      } else {
        // Automatically create a new tab if last tab is closed
        await this.createTab();
      }
    }
  }

  async selectTab(tabId: number): Promise<void> {
    if (this.tabs.has(tabId)) {
      this.activeTabId = tabId;
      this.emit('tab-activated', { tabId });
    }
  }

  async navigate(url: string, tabId?: number): Promise<void> {
    const targetId = tabId ?? this.activeTabId;
    const tab = this.tabs.get(targetId);
    if (!tab) return;

    tab.url = url;
    tab.isLoading = true;
    tab.progress = 0.2;
    this.emit('url-changed', { tabId: targetId, url });
    this.emit('loading-state-changed', { tabId: targetId, isLoading: true });
    this.emit('progress-changed', { tabId: targetId, progress: 0.2 });

    // Simulate page load completion
    setTimeout(() => {
      tab.isLoading = false;
      tab.progress = 1.0;
      tab.title = url.startsWith('http') ? new URL(url).hostname : 'Local Page';
      tab.securityLevel = url.startsWith('https://') ? 'SECURE' : 'LOCAL';
      tab.securityHost = url.startsWith('http') ? new URL(url).hostname : undefined;

      if (url.startsWith('http')) {
        this.history.unshift({
          id: String(Date.now()),
          title: tab.title,
          url,
          visitedAt: Date.now()
        });
      }

      this.emit('title-changed', { tabId: targetId, title: tab.title });
      this.emit('progress-changed', { tabId: targetId, progress: 1.0 });
      this.emit('loading-state-changed', { tabId: targetId, isLoading: false });
      this.emit('security-state-changed', {
        tabId: targetId,
        level: tab.securityLevel,
        host: tab.securityHost ?? ''
      });
      this.emit('shield-metrics-updated', { ...this.shieldMetrics });
    }, 150);
  }

  async reload(tabId?: number): Promise<void> {
    const targetId = tabId ?? this.activeTabId;
    const tab = this.tabs.get(targetId);
    if (tab) {
      await this.navigate(tab.url, targetId);
    }
  }

  async goBack(tabId?: number): Promise<void> {
    // In mock, reload current
    await this.reload(tabId);
  }

  async goForward(tabId?: number): Promise<void> {
    await this.reload(tabId);
  }

  async createGroup(title: string, color: string, tabIds: number[]): Promise<TabGroupInfo> {
    const id = 'group_' + Date.now();
    const group: TabGroupInfo = { id, title, color, tabIds };
    this.groups.set(id, group);

    for (const tabId of tabIds) {
      const tab = this.tabs.get(tabId);
      if (tab) {
        tab.groupId = id;
        tab.groupTitle = title;
        tab.groupColor = color;
      }
    }
    return group;
  }

  async renameGroup(groupId: string, title: string, color: string): Promise<void> {
    const group = this.groups.get(groupId);
    if (group) {
      group.title = title;
      group.color = color;
      for (const tabId of group.tabIds) {
        const tab = this.tabs.get(tabId);
        if (tab) {
          tab.groupTitle = title;
          tab.groupColor = color;
        }
      }
    }
  }

  async closeGroup(groupId: string): Promise<void> {
    const group = this.groups.get(groupId);
    if (group) {
      for (const tabId of [...group.tabIds]) {
        await this.closeTab(tabId);
      }
      this.groups.delete(groupId);
    }
  }

  async toggleShield(): Promise<boolean> {
    this.shieldMetrics.isShieldActive = !this.shieldMetrics.isShieldActive;
    this.emit('shield-metrics-updated', { ...this.shieldMetrics });
    return this.shieldMetrics.isShieldActive;
  }

  async getShieldMetrics(): Promise<ShieldMetrics> {
    return { ...this.shieldMetrics };
  }

  async toggleTor(): Promise<boolean> {
    this.torCircuit.isActive = !this.torCircuit.isActive;
    this.emit('tor-circuit-updated', { ...this.torCircuit });
    return this.torCircuit.isActive;
  }

  async getTorCircuit(): Promise<TorCircuitState> {
    return { ...this.torCircuit };
  }

  async getBookmarks(): Promise<BookmarkItem[]> {
    return [...this.bookmarks];
  }

  async addBookmark(title: string, url: string): Promise<BookmarkItem> {
    const item: BookmarkItem = {
      id: String(Date.now()),
      title,
      url,
      favicon: '⭐'
    };
    this.bookmarks.push(item);
    return item;
  }

  async removeBookmark(id: string): Promise<void> {
    this.bookmarks = this.bookmarks.filter(b => b.id !== id);
  }

  private history: HistoryItem[] = [];

  async getHistory(_query?: string): Promise<HistoryItem[]> {
    return [...this.history];
  }

  async getDownloads(): Promise<DownloadItem[]> {
    return [];
  }

  async minimizeWindow(): Promise<void> {
    console.log('[MockBridge] Minimize window');
  }

  async maximizeWindow(): Promise<void> {
    console.log('[MockBridge] Maximize window');
  }

  async closeWindow(): Promise<void> {
    console.log('[MockBridge] Close window');
  }

  on<K extends keyof HostEventMap>(event: K, listener: HostEventMap[K]): void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)!.add(listener);
  }

  off<K extends keyof HostEventMap>(event: K, listener: HostEventMap[K]): void {
    const set = this.listeners.get(event);
    if (set) {
      set.delete(listener);
    }
  }

  private emit<K extends keyof HostEventMap>(event: K, ...args: Parameters<HostEventMap[K]>): void {
    const set = this.listeners.get(event);
    if (set) {
      for (const listener of set) {
        // @ts-ignore
        listener(...args);
      }
    }
  }

  // Helper for tests to inspect internal tabs
  _getAllTabs(): TabInfo[] {
    return Array.from(this.tabs.values());
  }

  _getActiveTabId(): number {
    return this.activeTabId;
  }
}
