/**
 * Bun Unit Tests: IPC Bridge & Contract Invariants
 */

import { describe, expect, it } from 'bun:test';
import { MockDesktopBridge } from '../src/bridge/mockBridge.ts';

describe('MockDesktopBridge IPC Contract', () => {
  it('should initialize with a default active tab', () => {
    const bridge = new MockDesktopBridge();
    const tabs = bridge._getAllTabs();
    expect(tabs.length).toBe(1);
    expect(tabs[0].id).toBe(1);
    expect(tabs[0].url).toBe('https://search.azokle.com');
    expect(bridge._getActiveTabId()).toBe(1);
  });

  it('should create new tabs with proper attributes', async () => {
    const bridge = new MockDesktopBridge();
    const newTab = await bridge.createTab('https://github.com', false, false);

    expect(newTab.id).toBe(2);
    expect(newTab.url).toBe('https://github.com');
    expect(bridge._getActiveTabId()).toBe(2);
    expect(bridge._getAllTabs().length).toBe(2);
  });

  it('should close tab and activate next available tab', async () => {
    const bridge = new MockDesktopBridge();
    const tab2 = await bridge.createTab('https://reddit.com');
    expect(bridge._getAllTabs().length).toBe(2);

    await bridge.closeTab(tab2.id);
    expect(bridge._getAllTabs().length).toBe(1);
    expect(bridge._getActiveTabId()).toBe(1);
  });

  it('should toggle shield protection and update metrics', async () => {
    const bridge = new MockDesktopBridge();
    const metricsBefore = await bridge.getShieldMetrics();
    expect(metricsBefore.isShieldActive).toBe(true);

    const toggled = await bridge.toggleShield();
    expect(toggled).toBe(false);

    const metricsAfter = await bridge.getShieldMetrics();
    expect(metricsAfter.isShieldActive).toBe(false);
  });

  it('should toggle Tor circuit and dispatch state', async () => {
    const bridge = new MockDesktopBridge();
    const torBefore = await bridge.getTorCircuit();
    expect(torBefore.isActive).toBe(false);

    const toggled = await bridge.toggleTor();
    expect(toggled).toBe(true);

    const torAfter = await bridge.getTorCircuit();
    expect(torAfter.isActive).toBe(true);
    expect(torAfter.hops.length).toBe(3);
    expect(torAfter.hops[0].isGuard).toBe(true);
  });
});
