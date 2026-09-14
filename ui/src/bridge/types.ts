/**
 * Azokle Browser - Shared Desktop UI
 * Typed Cross-Platform IPC Bridge Contracts
 */

export type SecurityLevel = 'SECURE' | 'WARNING' | 'INSECURE' | 'LOCAL';

export interface TabInfo {
  id: number;
  title: string;
  url: string;
  favicon?: string;
  isPrivate: boolean;
  isTor: boolean;
  isLoading: boolean;
  progress: number;
  securityLevel: SecurityLevel;
  securityHost?: string;
  groupId?: string;
  groupTitle?: string;
  groupColor?: string;
  isAudioPlaying?: boolean;
  isMuted?: boolean;
  isPinned?: boolean;
}

export interface TabGroupInfo {
  id: string;
  title: string;
  color: string;
  tabIds: number[];
}

export interface ShieldMetrics {
  totalBlocked: number;
  adsBlocked: number;
  trackersBlocked: number;
  httpsUpgrades: number;
  isShieldActive: boolean;
  fingerprintProtection: boolean;
  cookieBlocking: boolean;
}

export interface TorHop {
  name: string;
  ip: string;
  country: string;
  isGuard?: boolean;
  isExit?: boolean;
}

export interface TorCircuitState {
  isActive: boolean;
  hops: TorHop[];
}

export interface SearchEngine {
  id: string;
  name: string;
  searchUrl: string;
  suggestUrl?: string;
  icon: string;
}

export interface BookmarkItem {
  id: string;
  title: string;
  url: string;
  favicon?: string;
}

export interface HistoryItem {
  id: string;
  title: string;
  url: string;
  visitedAt: number;
}

export interface DownloadItem {
  id: string;
  filename: string;
  url: string;
  totalBytes: number;
  receivedBytes: number;
  state: 'DOWNLOADING' | 'COMPLETED' | 'CANCELLED' | 'FAILED';
}

/**
 * Event map dispatched from Native Host (C++/Swift) to Shared TypeScript UI.
 */
export interface HostEventMap {
  'tab-created': (tab: TabInfo) => void;
  'tab-closed': (data: { tabId: number }) => void;
  'tab-activated': (data: { tabId: number }) => void;
  'url-changed': (data: { tabId: number; url: string }) => void;
  'title-changed': (data: { tabId: number; title: string }) => void;
  'progress-changed': (data: { tabId: number; progress: number }) => void;
  'loading-state-changed': (data: { tabId: number; isLoading: boolean }) => void;
  'security-state-changed': (data: { tabId: number; level: SecurityLevel; host: string }) => void;
  'shield-metrics-updated': (metrics: ShieldMetrics) => void;
  'tor-circuit-updated': (circuit: TorCircuitState) => void;
}

/**
 * Commands dispatched from Shared TypeScript UI to Native Host.
 */
export interface IDesktopBridge {
  // Tabs
  createTab(url?: string, isPrivate?: boolean, isTor?: boolean): Promise<TabInfo>;
  closeTab(tabId: number): Promise<void>;
  selectTab(tabId: number): Promise<void>;
  navigate(url: string, tabId?: number): Promise<void>;
  reload(tabId?: number): Promise<void>;
  goBack(tabId?: number): Promise<void>;
  goForward(tabId?: number): Promise<void>;

  // Tab Groups
  createGroup(title: string, color: string, tabIds: number[]): Promise<TabGroupInfo>;
  renameGroup(groupId: string, title: string, color: string): Promise<void>;
  closeGroup(groupId: string): Promise<void>;

  // Shields & Security
  toggleShield(): Promise<boolean>;
  getShieldMetrics(): Promise<ShieldMetrics>;
  toggleTor(): Promise<boolean>;
  getTorCircuit(): Promise<TorCircuitState>;

  // Bookmarks & History
  getBookmarks(): Promise<BookmarkItem[]>;
  addBookmark(title: string, url: string): Promise<BookmarkItem>;
  removeBookmark(id: string): Promise<void>;
  getHistory(query?: string): Promise<HistoryItem[]>;
  getDownloads(): Promise<DownloadItem[]>;

  // Window Management
  minimizeWindow(): Promise<void>;
  maximizeWindow(): Promise<void>;
  closeWindow(): Promise<void>;

  // Event Listeners
  on<K extends keyof HostEventMap>(event: K, listener: HostEventMap[K]): void;
  off<K extends keyof HostEventMap>(event: K, listener: HostEventMap[K]): void;
}
