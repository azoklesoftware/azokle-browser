/**
 * Azokle Browser - Shared Desktop UI
 * Reactive Browser State Store (Shields, Tor, Theme)
 */

import type { IDesktopBridge, ShieldMetrics, TorCircuitState, BookmarkItem } from '../bridge/types.ts';

export type BrowserStoreListener = () => void;

export class BrowserStore {
  private bridge: IDesktopBridge;
  private listeners: Set<BrowserStoreListener> = new Set();

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
    hops: []
  };

  private bookmarks: BookmarkItem[] = [];
  private isShieldFlyoutOpen: boolean = false;
  private isTorModalOpen: boolean = false;
  private isMenuOpen: boolean = false;
  private themeMode: 'dark' | 'light' = 'dark';

  constructor(bridge: IDesktopBridge) {
    this.bridge = bridge;
    this.init();
  }

  private async init(): Promise<void> {
    this.bridge.on('shield-metrics-updated', (metrics: ShieldMetrics) => {
      this.shieldMetrics = metrics;
      this.notify();
    });

    this.bridge.on('tor-circuit-updated', (circuit: TorCircuitState) => {
      this.torCircuit = circuit;
      this.notify();
    });

    try {
      this.shieldMetrics = await this.bridge.getShieldMetrics();
      this.torCircuit = await this.bridge.getTorCircuit();
      this.bookmarks = await this.bridge.getBookmarks();
    } catch {
      // Running in headless or initialization state
    }
    this.notify();
  }

  subscribe(listener: BrowserStoreListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private notify(): void {
    for (const listener of this.listeners) {
      listener();
    }
  }

  getShieldMetrics(): ShieldMetrics {
    return this.shieldMetrics;
  }

  getTorCircuit(): TorCircuitState {
    return this.torCircuit;
  }

  getBookmarks(): BookmarkItem[] {
    return this.bookmarks;
  }

  isShieldOpen(): boolean {
    return this.isShieldFlyoutOpen;
  }

  setShieldOpen(open: boolean): void {
    this.isShieldFlyoutOpen = open;
    this.notify();
  }

  isTorOpen(): boolean {
    return this.isTorModalOpen;
  }

  setTorOpen(open: boolean): void {
    this.isTorModalOpen = open;
    this.notify();
  }

  isBrowserMenuOpen(): boolean {
    return this.isMenuOpen;
  }

  setMenuOpen(open: boolean): void {
    this.isMenuOpen = open;
    this.notify();
  }

  async toggleShield(): Promise<void> {
    const active = await this.bridge.toggleShield();
    this.shieldMetrics.isShieldActive = active;
    this.notify();
  }

  async toggleTor(): Promise<void> {
    const active = await this.bridge.toggleTor();
    this.torCircuit.isActive = active;
    this.notify();
  }

  isBookmarked(url: string): boolean {
    return this.bookmarks.some(b => b.url === url);
  }

  addBookmark(title: string, url: string, favicon?: string): BookmarkItem {
    const existing = this.bookmarks.find(b => b.url === url);
    if (existing) {
      return existing;
    }
    const item: BookmarkItem = {
      id: `bm-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      title,
      url,
      favicon
    };
    this.bookmarks.push(item);
    this.notify();
    return item;
  }

  toggleBookmark(item: BookmarkItem): boolean {
    const idx = this.bookmarks.findIndex(b => b.url === item.url);
    if (idx >= 0) {
      this.bookmarks.splice(idx, 1);
      this.notify();
      return false;
    } else {
      this.bookmarks.push(item);
      this.notify();
      return true;
    }
  }
}
