/**
 * Bun Unit Tests: TabStore Reactive State
 */

import { describe, expect, it } from 'bun:test';
import { MockDesktopBridge } from '../src/bridge/mockBridge.ts';
import { TabStore } from '../src/state/tabStore.ts';

describe('TabStore Reactive State Management', () => {
  it('should receive initial tab from bridge upon creation', async () => {
    const bridge = new MockDesktopBridge();
    const store = new TabStore(bridge);

    let notificationCount = 0;
    store.subscribe(() => {
      notificationCount++;
    });

    const tab = await store.openTab('https://en.wikipedia.org');
    expect(tab.url).toBe('https://en.wikipedia.org');
    expect(store.getTabs().length).toBeGreaterThanOrEqual(1);
    expect(store.getActiveTab()?.url).toBe('https://en.wikipedia.org');
    expect(notificationCount).toBeGreaterThan(0);
  });

  it('should switch active tab on selectTab', async () => {
    const bridge = new MockDesktopBridge();
    const store = new TabStore(bridge);

    const tab1 = await store.openTab('https://site1.com');
    const tab2 = await store.openTab('https://site2.com');

    expect(store.getActiveTabId()).toBe(tab2.id);

    await store.selectTab(tab1.id);
    expect(store.getActiveTabId()).toBe(tab1.id);
  });

  it('should handle tab navigation updates', async () => {
    const bridge = new MockDesktopBridge();
    const store = new TabStore(bridge);

    await store.openTab('https://old-site.com');
    await store.navigate('https://new-site.com');

    // Wait for simulated async navigation
    await new Promise(resolve => setTimeout(resolve, 200));

    expect(store.getActiveTab()?.url).toBe('https://new-site.com');
    expect(store.getActiveTab()?.securityLevel).toBe('SECURE');
  });
});
