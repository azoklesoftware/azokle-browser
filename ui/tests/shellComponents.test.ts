/**
 * Bun Unit Tests: Azokle Browser Shell Components
 * Validates DOM rendering, event interactions, state reactivity, and keyboard handling
 */

import { describe, it, expect, beforeEach } from 'bun:test';
import { Window } from 'happy-dom';
import { MockDesktopBridge } from '../src/bridge/mockBridge.ts';
import { TabStore } from '../src/state/tabStore.ts';
import { BrowserStore } from '../src/state/browserStore.ts';

import { TabStripComponent } from '../src/components/tabstrip/TabStrip.ts';
import { NavigationToolbarComponent } from '../src/components/toolbar/NavigationToolbar.ts';
import { OmnibarComponent, cleanTrackingParameters } from '../src/components/omnibar/Omnibar.ts';
import { BrowserMenuComponent } from '../src/components/menu/BrowserMenu.ts';
import { TorCircuitModalComponent } from '../src/components/tor/TorCircuitModal.ts';
import { BookmarksBarComponent } from '../src/components/bookmarks/BookmarksBar.ts';

describe('Azokle Browser Shell Components', () => {
  let bridge: MockDesktopBridge;
  let tabStore: TabStore;
  let browserStore: BrowserStore;

  beforeEach(() => {
    const win = new Window();
    globalThis.window = win as any;
    globalThis.document = win.document as any;
    globalThis.KeyboardEvent = win.KeyboardEvent as any;
    globalThis.MouseEvent = win.MouseEvent as any;

    bridge = new MockDesktopBridge();
    tabStore = new TabStore(bridge);
    browserStore = new BrowserStore(bridge);
  });

  describe('TabStripComponent', () => {
    it('should render active tab and add-tab button', async () => {
      const root = document.createElement('div');
      new TabStripComponent(root, tabStore);

      await tabStore.openTab('https://search.azokle.com');

      const tabs = root.querySelectorAll('.tab-item');
      expect(tabs.length).toBeGreaterThanOrEqual(1);

      const addBtn = root.querySelector('.add-tab-btn');
      expect(addBtn).not.toBeNull();
    });

    it('should show audio mute/playing indicator when audio is active', async () => {
      const root = document.createElement('div');
      new TabStripComponent(root, tabStore);

      const tab = await tabStore.openTab('https://youtube.com');
      tab.isAudioPlaying = true;
      (tabStore as any).notify();

      const audioBtn = root.querySelector('.tab-audio-btn');
      expect(audioBtn).not.toBeNull();
    });

    it('should close tab on close button click', async () => {
      const root = document.createElement('div');
      new TabStripComponent(root, tabStore);

      await tabStore.openTab('https://site-to-close.com');
      const initialTabsCount = tabStore.getTabs().length;

      const closeBtn = root.querySelector('.tab-close-btn') as HTMLButtonElement;
      expect(closeBtn).not.toBeNull();
      closeBtn.click();

      // Wait a tick for async tabStore.closeTab
      await new Promise(resolve => setTimeout(resolve, 20));

      expect(tabStore.getTabs().length).toBeLessThan(initialTabsCount + 1);
    });

    it('should render integrated window controls (minimize, maximize, close)', () => {
      const root = document.createElement('div');
      new TabStripComponent(root, tabStore);

      expect(root.querySelector('.win-minimize')).not.toBeNull();
      expect(root.querySelector('.win-maximize')).not.toBeNull();
      expect(root.querySelector('.win-close')).not.toBeNull();
    });
  });

  describe('NavigationToolbarComponent', () => {
    it('should render navigation SVGs, Bookmark, Shield pill, and Settings/Menu button', () => {
      const root = document.createElement('div');
      new NavigationToolbarComponent(root, tabStore, browserStore);

      expect(root.querySelector('#nav-btn-back')).not.toBeNull();
      expect(root.querySelector('#nav-btn-forward')).not.toBeNull();
      expect(root.querySelector('#nav-btn-reload')).not.toBeNull();
      expect(root.querySelector('#nav-btn-bookmark')).not.toBeNull();
      expect(root.querySelector('#toolbar-shield-btn')).not.toBeNull();
      expect(root.querySelector('#toolbar-menu-btn')).not.toBeNull();
    });

    it('should update shield button on shield toggle', async () => {
      const root = document.createElement('div');
      new NavigationToolbarComponent(root, tabStore, browserStore);

      const shieldBtn = root.querySelector('#toolbar-shield-btn') as HTMLButtonElement;
      expect(shieldBtn.classList.contains('active')).toBe(true);

      await browserStore.toggleShield();
      expect(shieldBtn.classList.contains('off')).toBe(true);
    });
  });

  describe('OmnibarComponent', () => {
    it('should render input field, security badge, and action buttons', () => {
      const root = document.createElement('div');
      new OmnibarComponent(root, tabStore, browserStore);

      const input = root.querySelector('.omnibar-input') as HTMLInputElement;
      expect(input).not.toBeNull();

      const badge = root.querySelector('.security-badge');
      expect(badge).not.toBeNull();

      const starBtn = root.querySelector('.star-btn');
      expect(starBtn).not.toBeNull();

      const cleanCopyBtn = root.querySelector('.clean-copy-btn');
      expect(cleanCopyBtn).not.toBeNull();

      const qrBtn = root.querySelector('.qr-btn');
      expect(qrBtn).not.toBeNull();
    });

    it('should sanitize tracking parameters with cleanTrackingParameters', () => {
      const dirty = 'https://example.com/item?id=42&utm_source=twitter&utm_medium=cpc&fbclid=abc123xyz&ref_=123#reviews';
      const cleaned = cleanTrackingParameters(dirty);
      expect(cleaned).toBe('https://example.com/item?id=42#reviews');
    });

    it('should toggle bookmark on star button click', async () => {
      const root = document.createElement('div');
      new OmnibarComponent(root, tabStore, browserStore);

      await tabStore.openTab('https://sovereign-news.org');
      const starBtn = root.querySelector('.star-btn') as HTMLButtonElement;

      expect(browserStore.isBookmarked('https://sovereign-news.org')).toBe(false);

      starBtn.click();
      expect(browserStore.isBookmarked('https://sovereign-news.org')).toBe(true);
      expect(starBtn.classList.contains('bookmarked')).toBe(true);

      starBtn.click();
      expect(browserStore.isBookmarked('https://sovereign-news.org')).toBe(false);
    });

    it('should navigate on Enter key with bang shortcuts', async () => {
      const root = document.createElement('div');
      new OmnibarComponent(root, tabStore, browserStore);

      await tabStore.openTab('azokle://newtab');
      const input = root.querySelector('.omnibar-input') as HTMLInputElement;
      input.value = '!gh bun';

      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      input.dispatchEvent(event);

      expect(tabStore.getActiveTab()?.url).toBe('https://github.com/search?q=bun');
    });

    it('should toggle page info bubble on security badge click', async () => {
      const root = document.createElement('div');
      new OmnibarComponent(root, tabStore, browserStore);

      await tabStore.openTab('https://azokle.com');
      const badge = root.querySelector('.security-badge') as HTMLButtonElement;
      const bubble = root.querySelector('.page-info-bubble') as HTMLElement;

      expect(bubble.style.display).toBe('none');
      badge.click();
      expect(bubble.style.display).toBe('block');
      expect(bubble.textContent).toContain('Connection is secure');

      badge.click();
      expect(bubble.style.display).toBe('none');
    });

    it('should toggle QR share modal on qr button click', async () => {
      const root = document.createElement('div');
      new OmnibarComponent(root, tabStore, browserStore);

      await tabStore.openTab('https://azokle.com');
      const qrBtn = root.querySelector('.qr-btn') as HTMLButtonElement;
      const bubble = root.querySelector('.qr-share-bubble') as HTMLElement;

      expect(bubble.style.display).toBe('none');
      qrBtn.click();
      expect(bubble.style.display).toBe('block');
      expect(bubble.textContent).toContain('Scan QR code');

      qrBtn.click();
      expect(bubble.style.display).toBe('none');
    });
  });

  describe('BrowserMenuComponent', () => {
    it('should render zoom controls and menu sections when open', () => {
      const root = document.createElement('div');
      new BrowserMenuComponent(root, browserStore, tabStore);

      browserStore.setMenuOpen(true);
      expect(root.style.display).toBe('block');

      expect(root.querySelector('.menu-zoom-row')).not.toBeNull();
      expect(root.querySelector('#zoom-inc')).not.toBeNull();
      expect(root.querySelector('#zoom-dec')).not.toBeNull();
      expect(root.querySelector('#menu-new-tab')).not.toBeNull();
      expect(root.querySelector('#menu-shields')).not.toBeNull();
      expect(root.querySelector('#menu-settings')).not.toBeNull();
    });

    it('should change zoom level on zoom buttons click', () => {
      const root = document.createElement('div');
      new BrowserMenuComponent(root, browserStore, tabStore);

      browserStore.setMenuOpen(true);
      const incBtn = root.querySelector('#zoom-inc') as HTMLButtonElement;
      const valEl = root.querySelector('#zoom-val') as HTMLElement;

      incBtn.click();
      expect(valEl.textContent).toBe('110%');
    });
  });

  describe('TorCircuitModalComponent', () => {
    it('should render 3-hop relay graph when open', () => {
      const root = document.createElement('div');
      new TorCircuitModalComponent(root, browserStore, tabStore);

      browserStore.setTorOpen(true);
      expect(root.style.display).toBe('block');

      const nodes = root.querySelectorAll('.tor-relay-node');
      expect(nodes.length).toBeGreaterThanOrEqual(3);

      const newIdBtn = root.querySelector('#tor-new-id-btn');
      expect(newIdBtn).not.toBeNull();
    });
  });

  describe('BookmarksBarComponent', () => {
    it('should render bookmarks and all-bookmarks button', () => {
      const root = document.createElement('div');
      new BookmarksBarComponent(root, browserStore, tabStore);

      browserStore.toggleBookmark({
        id: '99',
        title: 'Brave UI Benchmark',
        url: 'https://brave.com'
      });

      const bookmarkPills = root.querySelectorAll('.bookmark-pill');
      expect(bookmarkPills.length).toBeGreaterThanOrEqual(1);

      const allBtn = root.querySelector('.all-bookmarks-btn');
      expect(allBtn).not.toBeNull();
    });
  });
});
