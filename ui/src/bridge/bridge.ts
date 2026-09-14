/**
 * Azokle Browser - Shared Desktop UI
 * Production IPC Bridge with Native WebKitGTK / WKWebView / WebView2 Dispatcher
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
import { MockDesktopBridge } from './mockBridge.ts';

declare global {
  interface Window {
    webkit?: {
      messageHandlers?: {
        azokleNative?: {
          postMessage: (message: any) => void;
        };
      };
    };
    chrome?: {
      webview?: {
        postMessage: (message: any) => void;
        addEventListener: (type: string, listener: (ev: any) => void) => void;
      };
    };
    azokleDesktopBridge?: IDesktopBridge;
    __azokleHostDispatch?: (event: string, payload: any) => void;
  }
}

class NativeDesktopBridge implements IDesktopBridge {
  private listeners: Map<string, Set<Function>> = new Map();
  private pendingRequests: Map<string, { resolve: Function; reject: Function }> = new Map();
  private requestIdCounter = 1;

  constructor() {
    this.setupHostDispatcher();
  }

  private setupHostDispatcher(): void {
    // Expose global hook for C++ (WebKitGTK) / Swift (WKWebView) to push events into JS
    window.__azokleHostDispatch = (event: string, payload: any) => {
      if (event === 'response') {
        const pending = this.pendingRequests.get(payload.id);
        if (pending) {
          this.pendingRequests.delete(payload.id);
          if (payload.error) pending.reject(new Error(payload.error));
          else pending.resolve(payload.result);
        }
        return;
      }

      const set = this.listeners.get(event);
      if (set) {
        for (const listener of set) {
          listener(payload);
        }
      }
    };

    // Listen for WebView2 on Windows
    if (window.chrome?.webview?.addEventListener) {
      window.chrome.webview.addEventListener('message', (event: any) => {
        const data = event.data;
        if (data && data.event && window.__azokleHostDispatch) {
          window.__azokleHostDispatch(data.event, data.payload);
        }
      });
    }
  }

  private postToNative(action: string, args: any = {}): Promise<any> {
    const id = 'req_' + this.requestIdCounter++;
    return new Promise((resolve, reject) => {
      this.pendingRequests.set(id, { resolve, reject });

      const envelope = { id, action, ...args };

      if (window.webkit?.messageHandlers?.azokleNative?.postMessage) {
        window.webkit.messageHandlers.azokleNative.postMessage(envelope);
      } else if (window.chrome?.webview?.postMessage) {
        window.chrome.webview.postMessage(envelope);
      } else {
        this.pendingRequests.delete(id);
        reject(new Error('No native desktop IPC handler detected'));
      }
    });
  }

  async createTab(url?: string, isPrivate?: boolean, isTor?: boolean): Promise<TabInfo> {
    return this.postToNative('createTab', { url, isPrivate, isTor });
  }

  async closeTab(tabId: number): Promise<void> {
    return this.postToNative('closeTab', { tabId });
  }

  async selectTab(tabId: number): Promise<void> {
    return this.postToNative('selectTab', { tabId });
  }

  async navigate(url: string, tabId?: number): Promise<void> {
    return this.postToNative('navigate', { url, tabId });
  }

  async reload(tabId?: number): Promise<void> {
    return this.postToNative('reload', { tabId });
  }

  async goBack(tabId?: number): Promise<void> {
    return this.postToNative('goBack', { tabId });
  }

  async goForward(tabId?: number): Promise<void> {
    return this.postToNative('goForward', { tabId });
  }

  async createGroup(title: string, color: string, tabIds: number[]): Promise<TabGroupInfo> {
    return this.postToNative('createGroup', { title, color, tabIds });
  }

  async renameGroup(groupId: string, title: string, color: string): Promise<void> {
    return this.postToNative('renameGroup', { groupId, title, color });
  }

  async closeGroup(groupId: string): Promise<void> {
    return this.postToNative('closeGroup', { groupId });
  }

  async toggleShield(): Promise<boolean> {
    return this.postToNative('toggleShield');
  }

  async getShieldMetrics(): Promise<ShieldMetrics> {
    return this.postToNative('getShieldMetrics');
  }

  async toggleTor(): Promise<boolean> {
    return this.postToNative('toggleTor');
  }

  async getTorCircuit(): Promise<TorCircuitState> {
    return this.postToNative('getTorCircuit');
  }

  async getBookmarks(): Promise<BookmarkItem[]> {
    return this.postToNative('getBookmarks');
  }

  async addBookmark(title: string, url: string): Promise<BookmarkItem> {
    return this.postToNative('addBookmark', { title, url });
  }

  async removeBookmark(id: string): Promise<void> {
    return this.postToNative('removeBookmark', { id });
  }

  async getHistory(query?: string): Promise<HistoryItem[]> {
    return this.postToNative('getHistory', { query });
  }

  async getDownloads(): Promise<DownloadItem[]> {
    return this.postToNative('getDownloads');
  }

  async minimizeWindow(): Promise<void> {
    return this.postToNative('minimizeWindow');
  }

  async maximizeWindow(): Promise<void> {
    return this.postToNative('maximizeWindow');
  }

  async closeWindow(): Promise<void> {
    return this.postToNative('closeWindow');
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
}

/**
 * Singleton Bridge instance:
 * Automatically uses NativeDesktopBridge if running inside WebKitGTK / WKWebView / WebView2,
 * or MockDesktopBridge if running in browser / dev / test environments.
 */
let ssrBridge: IDesktopBridge | null = null;

export function getDesktopBridge(): IDesktopBridge {
  if (typeof window === 'undefined') {
    if (!ssrBridge) {
      ssrBridge = new MockDesktopBridge();
    }
    return ssrBridge;
  }

  if (!window.azokleDesktopBridge) {
    const hasNativeHost =
      Boolean(window.webkit?.messageHandlers?.azokleNative) ||
      Boolean(window.chrome?.webview);

    if (hasNativeHost) {
      window.azokleDesktopBridge = new NativeDesktopBridge();
    } else {
      window.azokleDesktopBridge = new MockDesktopBridge();
    }
  }
  return window.azokleDesktopBridge;
}
