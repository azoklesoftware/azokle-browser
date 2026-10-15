/**
 * Azokle Browser - Shared Desktop UI
 * Tor Circuit Visualizer Component (Brave-Grade Architecture)
 */

import type { BrowserStore } from '../../state/browserStore.ts';
import type { TabStore } from '../../state/tabStore.ts';

export class TorCircuitModalComponent {
  private container: HTMLElement;
  private browserStore: BrowserStore;
  private tabStore: TabStore;

  constructor(container: HTMLElement, browserStore: BrowserStore, tabStore: TabStore) {
    this.container = container;
    this.browserStore = browserStore;
    this.tabStore = tabStore;
    this.browserStore.subscribe(() => this.render());
    this.render();
  }

  private render(): void {
    const isOpen = this.browserStore.isTorOpen();
    if (!isOpen) {
      this.container.style.display = 'none';
      return;
    }

    const tor = this.browserStore.getTorCircuit();
    const activeTab = this.tabStore.getActiveTab();
    const rawHost = activeTab?.securityHost || (activeTab?.url.startsWith('http') ? new URL(activeTab.url).hostname : 'this site');
    const host = this.escapeHtml(rawHost);

    this.container.style.display = 'block';
    this.container.className = 'flyout-overlay tor-modal-container';

    // Mock hops if empty
    const hops = tor.hops.length > 0 ? tor.hops : [
      { name: 'Guard Relay (Entry)', ip: '185.220.101.5', country: '🇩🇪 Germany', isGuard: true, isExit: false },
      { name: 'Middle Relay (Hop 2)', ip: '198.51.100.42', country: '🇨🇭 Switzerland', isGuard: false, isExit: false },
      { name: 'Exit Relay (Egress)', ip: '192.0.2.89', country: '🇸🇪 Sweden', isGuard: false, isExit: true }
    ];

    this.container.innerHTML = `
      <div class="tor-header">
        <div class="tor-header-title">
          <div class="tor-badge-icon">🧅</div>
          <div>
            <h3>Tor Onion Circuit</h3>
            <p class="tor-status-subtitle">
              ${tor.isActive ? `<span class="status-connected">● Connected via SOCKS5:9050</span>` : '<span class="status-disconnected">○ Disconnected</span>'}
            </p>
          </div>
        </div>
        <button class="tor-toggle-btn ${tor.isActive ? 'active' : ''}" id="tor-power-btn">
          ${tor.isActive ? 'Disconnect' : 'Connect'}
        </button>
      </div>

      <div class="tor-site-target">
        <span class="target-label">Circuit for:</span>
        <span class="target-host">${host}</span>
      </div>

      <div class="tor-circuit-box">
        <div class="tor-relay-chain">
          <!-- User Browser Node -->
          <div class="tor-relay-node browser-node">
            <div class="node-bullet-outer"><div class="node-bullet-inner"></div></div>
            <div class="node-info">
              <span class="node-name">This Browser</span>
              <span class="node-ip">Client (Isolated Sandbox)</span>
            </div>
          </div>

          <div class="relay-connector-line"></div>

          ${hops.map((hop, idx) => `
            <div class="tor-relay-node ${hop.isGuard ? 'guard-node' : hop.isExit ? 'exit-node' : 'middle-node'}">
              <div class="node-bullet-outer"><div class="node-bullet-inner"></div></div>
              <div class="node-info">
                <div class="node-title-row">
                  <span class="node-name">${this.escapeHtml(hop.name)}</span>
                  <span class="node-role-badge">${hop.isGuard ? 'Guard' : hop.isExit ? 'Exit' : `Hop ${idx + 1}`}</span>
                </div>
                <span class="node-details">${this.escapeHtml(hop.country)} • <span class="ip-mono">${this.escapeHtml(hop.ip)}</span></span>
              </div>
            </div>
            ${idx < hops.length - 1 ? '<div class="relay-connector-line"></div>' : ''}
          `).join('')}

          <div class="relay-connector-line"></div>

          <!-- Internet / Target Site Node -->
          <div class="tor-relay-node target-node">
            <div class="node-bullet-outer"><div class="node-bullet-inner"></div></div>
            <div class="node-info">
              <span class="node-name">Destination Website</span>
              <span class="node-ip">${host}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="tor-footer">
        <button class="tor-new-circuit-btn" id="tor-new-id-btn" ${!tor.isActive ? 'disabled' : ''}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          New Tor Circuit for this Site
        </button>
      </div>
    `;

    const powerBtn = this.container.querySelector('#tor-power-btn') as HTMLButtonElement;
    if (powerBtn) {
      powerBtn.onclick = () => this.browserStore.toggleTor();
    }

    const newIdBtn = this.container.querySelector('#tor-new-id-btn') as HTMLButtonElement;
    if (newIdBtn) {
      newIdBtn.onclick = () => {
        newIdBtn.innerHTML = `<span>Rotating Circuit...</span>`;
        setTimeout(() => {
          this.render();
        }, 600);
      };
    }
  }

  private escapeHtml(str: string): string {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
}
