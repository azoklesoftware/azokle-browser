/**
 * Azokle Browser - Shared Desktop UI
 * Brave-Grade Omnibar Address & Search Capsule (Brave-Core LocationBarView Architecture)
 */

import type { TabStore } from '../../state/tabStore.ts';
import type { BrowserStore } from '../../state/browserStore.ts';
import type { SecurityLevel } from '../../bridge/types.ts';

export interface SuggestionItem {
  type: 'search' | 'bang' | 'bookmark' | 'history' | 'navigate';
  title: string;
  subtitle?: string;
  url: string;
  badge?: string;
  icon: string;
}

/**
 * Strips tracking parameters from a URL (equivalent to Brave's CopySanitizedURL)
 */
export function cleanTrackingParameters(rawUrl: string): string {
  try {
    const parsed = new URL(rawUrl);
    const trackingParams = new Set([
      'utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'utm_id',
      'utm_source_platform', 'utm_creative_format', 'utm_marketing_tactic',
      'fbclid', 'gclid', 'gclsrc', 'dclid', 'wbraid', 'gbraid', 'msclkid',
      'mc_eid', 'mc_cid', 'igshid', 'yclid', 'wickedid',
      '_hsenc', '_hsmi', 'hsctstracking', 'mkt_tok', 'trk', 'si', 'twclid',
      'ref_', 'fbadid'
    ]);

    const toDelete: string[] = [];
    for (const key of parsed.searchParams.keys()) {
      const lower = key.toLowerCase();
      if (trackingParams.has(lower) || lower.startsWith('utm_') || lower.startsWith('fb_')) {
        toDelete.push(key);
      }
    }
    for (const param of toDelete) {
      parsed.searchParams.delete(param);
    }
    return parsed.toString();
  } catch {
    return rawUrl;
  }
}

export class OmnibarComponent {
  private container: HTMLElement;
  private tabStore: TabStore;
  private browserStore?: BrowserStore;

  // Leading Views
  private leadingContainerEl!: HTMLElement;
  private securityBadgeEl!: HTMLElement;
  private searchEngineBadgeEl!: HTMLElement;

  // Center Views
  private inputWrapperEl!: HTMLElement;
  private inputEl!: HTMLInputElement;

  // Trailing Actions Container (BraveActionsContainer / PageActionIconView)
  private actionsEl!: HTMLElement;
  private zoomBtn!: HTMLButtonElement;
  private qrBtn!: HTMLButtonElement;
  private cleanCopyBtn!: HTMLButtonElement;
  private starBtn!: HTMLButtonElement;

  // Floating Popovers
  private dropdownEl!: HTMLElement;
  private pageInfoBubbleEl!: HTMLElement;
  private qrShareBubbleEl!: HTMLElement;

  // State
  private isDropdownOpen: boolean = false;
  private isPageInfoOpen: boolean = false;
  private isQrModalOpen: boolean = false;
  private selectedIndex: number = -1;
  private suggestions: SuggestionItem[] = [];
  private currentZoomLevel: number = 100;

  constructor(container: HTMLElement, tabStore: TabStore, browserStore?: BrowserStore) {
    this.container = container;
    this.tabStore = tabStore;
    this.browserStore = browserStore;

    this.render();
    this.tabStore.subscribe(() => this.updateState());
    if (this.browserStore) {
      this.browserStore.subscribe(() => {
        this.updateBookmarkStar();
        this.updateZoomBadge();
      });
    }
  }

  private render(): void {
    this.container.className = 'omnibar-container';
    this.container.innerHTML = '';

    // ==========================================
    // 1. LEADING SECTION (Security Lock / Tune & Search Chip)
    // ==========================================
    this.leadingContainerEl = document.createElement('div');
    this.leadingContainerEl.className = 'omnibar-leading';

    this.securityBadgeEl = document.createElement('button');
    this.securityBadgeEl.className = 'security-badge secure';
    this.securityBadgeEl.title = 'View site information and security';
    this.securityBadgeEl.setAttribute('aria-label', 'Site Security');
    this.securityBadgeEl.innerHTML = this.getSecurityIcon('SECURE');
    this.securityBadgeEl.onclick = (e) => {
      e.stopPropagation();
      this.togglePageInfoBubble();
    };

    this.searchEngineBadgeEl = document.createElement('span');
    this.searchEngineBadgeEl.className = 'omnibar-search-chip';
    this.searchEngineBadgeEl.innerHTML = `
      <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
      </svg>
      <span>Azokle</span>
    `;
    this.searchEngineBadgeEl.style.display = 'none';

    this.leadingContainerEl.appendChild(this.securityBadgeEl);
    this.leadingContainerEl.appendChild(this.searchEngineBadgeEl);

    // ==========================================
    // 2. CENTER SECTION (Address & Search Input)
    // ==========================================
    this.inputWrapperEl = document.createElement('div');
    this.inputWrapperEl.className = 'omnibar-input-wrapper';

    this.inputEl = document.createElement('input');
    this.inputEl.type = 'text';
    this.inputEl.className = 'omnibar-input';
    this.inputEl.placeholder = 'Search with Azokle or enter URL...';
    this.inputEl.autocomplete = 'off';
    this.inputEl.spellcheck = false;

    this.inputWrapperEl.appendChild(this.inputEl);

    // ==========================================
    // 3. TRAILING ACTIONS CONTAINER (Brave Actions)
    // ==========================================
    this.actionsEl = document.createElement('div');
    this.actionsEl.className = 'omnibar-actions';

    // Zoom Indicator / Reset Button
    this.zoomBtn = document.createElement('button');
    this.zoomBtn.className = 'omnibar-action-btn zoom-btn';
    this.zoomBtn.title = 'Reset zoom to 100%';
    this.zoomBtn.textContent = '100%';
    this.zoomBtn.style.display = 'none';
    this.zoomBtn.onclick = (e) => {
      e.stopPropagation();
      this.resetZoom();
    };

    // QR Code Share Button
    this.qrBtn = document.createElement('button');
    this.qrBtn.className = 'omnibar-action-btn qr-btn';
    this.qrBtn.title = 'Share this page via QR code';
    this.qrBtn.setAttribute('aria-label', 'Share QR Code');
    this.qrBtn.innerHTML = `
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="3" width="7" height="7"/>
        <rect x="14" y="3" width="7" height="7"/>
        <rect x="14" y="14" width="7" height="7"/>
        <rect x="3" y="14" width="7" height="7"/>
      </svg>
    `;
    this.qrBtn.onclick = (e) => {
      e.stopPropagation();
      this.toggleQrShareBubble();
    };

    // Copy Clean Link Button (Brave signature: CleanAndCopySelectedURL)
    this.cleanCopyBtn = document.createElement('button');
    this.cleanCopyBtn.className = 'omnibar-action-btn clean-copy-btn';
    this.cleanCopyBtn.title = 'Copy clean link (strips trackers)';
    this.cleanCopyBtn.setAttribute('aria-label', 'Copy Clean Link');
    this.cleanCopyBtn.innerHTML = `
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
        <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
      </svg>
    `;
    this.cleanCopyBtn.onclick = (e) => {
      e.stopPropagation();
      this.copyCleanLink();
    };

    // Bookmark Star Button
    this.starBtn = document.createElement('button');
    this.starBtn.className = 'omnibar-action-btn star-btn';
    this.starBtn.title = 'Bookmark this tab (Ctrl+D)';
    this.starBtn.setAttribute('aria-label', 'Bookmark Tab');
    this.starBtn.innerHTML = `
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
      </svg>
    `;
    this.starBtn.onclick = (e) => {
      e.stopPropagation();
      this.toggleBookmark();
    };

    this.actionsEl.appendChild(this.zoomBtn);
    this.actionsEl.appendChild(this.qrBtn);
    this.actionsEl.appendChild(this.cleanCopyBtn);
    this.actionsEl.appendChild(this.starBtn);

    // ==========================================
    // 4. FLOATING POPOVERS
    // ==========================================
    // BraveRoundedOmniboxResultsFrame
    this.dropdownEl = document.createElement('div');
    this.dropdownEl.className = 'omnibar-dropdown';
    this.dropdownEl.style.display = 'none';

    // Page Info Bubble (BraveShieldsPageInfoController)
    this.pageInfoBubbleEl = document.createElement('div');
    this.pageInfoBubbleEl.className = 'page-info-bubble';
    this.pageInfoBubbleEl.style.display = 'none';

    // QR Share Bubble
    this.qrShareBubbleEl = document.createElement('div');
    this.qrShareBubbleEl.className = 'qr-share-bubble';
    this.qrShareBubbleEl.style.display = 'none';

    // ==========================================
    // 5. EVENT LISTENERS
    // ==========================================
    this.inputEl.addEventListener('input', () => {
      this.handleInput(this.inputEl.value);
    });

    this.inputEl.addEventListener('focus', () => {
      this.inputEl.select();
      this.closePageInfoBubble();
      this.closeQrShareBubble();
      this.handleInput(this.inputEl.value);
    });

    this.inputEl.addEventListener('keydown', (e) => {
      this.handleKeydown(e);
    });

    // Dismiss popups on click outside
    document.addEventListener('click', (e) => {
      if (!this.container.contains(e.target as Node)) {
        this.closeDropdown();
        this.closePageInfoBubble();
        this.closeQrShareBubble();
      }
    });

    // Keyboard shortcut: Ctrl+D / Cmd+D for Bookmark
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'd' && !e.shiftKey && !e.altKey) {
        e.preventDefault();
        this.toggleBookmark();
      }
    });

    // Append all nodes
    this.container.appendChild(this.leadingContainerEl);
    this.container.appendChild(this.inputWrapperEl);
    this.container.appendChild(this.actionsEl);
    this.container.appendChild(this.dropdownEl);
    this.container.appendChild(this.pageInfoBubbleEl);
    this.container.appendChild(this.qrShareBubbleEl);

    this.updateState();
  }

  // =======================================================
  // SUGGESTIONS & AUTOCOMPLETE (BraveOmniboxResultView)
  // =======================================================
  private handleInput(value: string): void {
    const q = value.trim();
    this.suggestions = [];
    this.selectedIndex = -1;

    if (!q) {
      this.searchEngineBadgeEl.style.display = 'none';

      // Default quick shortcuts & popular search bangs
      this.suggestions = [
        {
          type: 'bang',
          title: '!g <query>',
          subtitle: 'Search Google directly',
          url: 'https://google.com',
          badge: 'Search Bang',
          icon: '🔍'
        },
        {
          type: 'bang',
          title: '!gh <query>',
          subtitle: 'Search GitHub repositories & code',
          url: 'https://github.com',
          badge: 'Developer',
          icon: '🐙'
        },
        {
          type: 'bang',
          title: '!yt <query>',
          subtitle: 'Search YouTube videos',
          url: 'https://youtube.com',
          badge: 'Media',
          icon: '▶️'
        },
        {
          type: 'bang',
          title: '!w <query>',
          subtitle: 'Search Wikipedia encyclopedia',
          url: 'https://wikipedia.org',
          badge: 'Knowledge',
          icon: '📚'
        },
        {
          type: 'bang',
          title: '!ddg <query>',
          subtitle: 'Search DuckDuckGo private search',
          url: 'https://duckduckgo.com',
          badge: 'Privacy',
          icon: '🦆'
        }
      ];
    } else {
      this.searchEngineBadgeEl.style.display = 'inline-flex';

      // 1. Direct Azokle Search
      this.suggestions.push({
        type: 'search',
        title: q,
        subtitle: 'Search Azokle Sovereign Index',
        url: `https://search.azokle.com/search?q=${encodeURIComponent(q)}`,
        badge: 'Azokle Search',
        icon: '🌐'
      });

      // 2. Direct Navigation Match (if domain-like)
      if (q.includes('.') && !q.includes(' ') && !q.startsWith('!')) {
        const fullUrl = q.startsWith('http://') || q.startsWith('https://') ? q : `https://${q}`;
        this.suggestions.push({
          type: 'navigate',
          title: q,
          subtitle: 'Navigate to address',
          url: fullUrl,
          badge: 'Visit Domain',
          icon: '🔗'
        });
      }

      // 3. Bang shortcuts
      const bangs = [
        { prefix: '!g', name: 'Google Search', urlTpl: 'https://www.google.com/search?q=', badge: 'Google' },
        { prefix: '!gh', name: 'GitHub Search', urlTpl: 'https://github.com/search?q=', badge: 'GitHub' },
        { prefix: '!yt', name: 'YouTube Search', urlTpl: 'https://www.youtube.com/results?search_query=', badge: 'YouTube' },
        { prefix: '!w', name: 'Wikipedia Search', urlTpl: 'https://en.wikipedia.org/wiki/Special:Search?search=', badge: 'Wiki' },
        { prefix: '!ddg', name: 'DuckDuckGo Search', urlTpl: 'https://duckduckgo.com/?q=', badge: 'DDG' }
      ];

      for (const b of bangs) {
        if (q.startsWith(b.prefix) || b.prefix.startsWith(q)) {
          const rawQuery = q.slice(b.prefix.length).trim();
          this.suggestions.push({
            type: 'bang',
            title: `${b.prefix} ${rawQuery || '<search terms>'}`,
            subtitle: b.name,
            url: rawQuery ? `${b.urlTpl}${encodeURIComponent(rawQuery)}` : b.urlTpl,
            badge: b.badge,
            icon: '⚡'
          });
        }
      }

      // 4. Bookmarks Matching
      if (this.browserStore) {
        const lowerQ = q.toLowerCase();
        const matched = this.browserStore.getBookmarks().filter(
          b => b.title.toLowerCase().includes(lowerQ) || b.url.toLowerCase().includes(lowerQ)
        );
        for (const bm of matched.slice(0, 3)) {
          this.suggestions.push({
            type: 'bookmark',
            title: bm.title,
            subtitle: bm.url,
            url: bm.url,
            badge: 'Bookmark',
            icon: '★'
          });
        }
      }
    }

    this.renderDropdown();
  }

  private handleKeydown(e: KeyboardEvent): void {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (this.suggestions.length > 0) {
        this.selectedIndex = (this.selectedIndex + 1) % this.suggestions.length;
        this.renderDropdown();
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (this.suggestions.length > 0) {
        this.selectedIndex = (this.selectedIndex - 1 + this.suggestions.length) % this.suggestions.length;
        this.renderDropdown();
      }
    } else if (e.key === 'Enter') {
      if (this.selectedIndex >= 0 && this.suggestions[this.selectedIndex]) {
        this.navigateToUrl(this.suggestions[this.selectedIndex].url);
      } else {
        const val = this.inputEl.value.trim();
        if (val) this.navigateToQuery(val);
      }
      this.closeDropdown();
      this.inputEl.blur();
    } else if (e.key === 'Escape') {
      this.closeDropdown();
      this.closePageInfoBubble();
      this.closeQrShareBubble();
      this.updateState();
      this.inputEl.blur();
    }
  }

  private renderDropdown(): void {
    if (this.suggestions.length === 0) {
      this.closeDropdown();
      return;
    }

    this.dropdownEl.innerHTML = '';
    this.dropdownEl.style.display = 'block';
    this.container.classList.add('dropdown-open');
    this.isDropdownOpen = true;

    const listEl = document.createElement('div');
    listEl.className = 'omnibar-results-list';

    this.suggestions.forEach((item, idx) => {
      const row = document.createElement('div');
      row.className = `suggestion-row ${idx === this.selectedIndex ? 'selected' : ''}`;
      row.innerHTML = `
        <span class="suggestion-icon ${item.type}">${item.icon}</span>
        <div class="suggestion-text">
          <span class="suggestion-title">${this.escapeHtml(item.title)}</span>
          ${item.subtitle ? `<span class="suggestion-subtitle">${this.escapeHtml(item.subtitle)}</span>` : ''}
        </div>
        ${item.badge ? `<span class="suggestion-badge">${this.escapeHtml(item.badge)}</span>` : ''}
      `;

      row.onmouseenter = () => {
        this.selectedIndex = idx;
        const allRows = listEl.querySelectorAll('.suggestion-row');
        allRows.forEach((r, i) => r.classList.toggle('selected', i === idx));
      };

      row.onmousedown = (e) => {
        e.preventDefault();
        this.navigateToUrl(item.url);
        this.closeDropdown();
      };

      listEl.appendChild(row);
    });

    // Brave-style Bottom Footer with Keyboard Hints
    const footerEl = document.createElement('div');
    footerEl.className = 'omnibar-results-footer';
    footerEl.innerHTML = `
      <div class="footer-hint"><kbd>↑</kbd><kbd>↓</kbd> navigate</div>
      <div class="footer-hint"><kbd>↵</kbd> select</div>
      <div class="footer-hint"><kbd>Esc</kbd> dismiss</div>
    `;

    this.dropdownEl.appendChild(listEl);
    this.dropdownEl.appendChild(footerEl);
  }

  private closeDropdown(): void {
    this.dropdownEl.style.display = 'none';
    this.container.classList.remove('dropdown-open');
    this.isDropdownOpen = false;
    this.selectedIndex = -1;
  }

  // =======================================================
  // COPY CLEAN LINK (CleanAndCopySelectedURL / CopySanitizedURL)
  // =======================================================
  private copyCleanLink(): void {
    const active = this.tabStore.getActiveTab();
    if (!active || !active.url) return;

    const sanitized = cleanTrackingParameters(active.url);
    navigator.clipboard?.writeText(sanitized);

    // Provide visual feedback checkmark
    const originalHtml = this.cleanCopyBtn.innerHTML;
    this.cleanCopyBtn.innerHTML = `
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="20 6 9 17 4 12"/>
      </svg>
    `;
    this.cleanCopyBtn.classList.add('copied');
    this.cleanCopyBtn.title = 'Clean link copied!';

    setTimeout(() => {
      this.cleanCopyBtn.innerHTML = originalHtml;
      this.cleanCopyBtn.classList.remove('copied');
      this.cleanCopyBtn.title = 'Copy clean link (strips trackers)';
    }, 1800);
  }

  // =======================================================
  // BOOKMARK STAR TOGGLE
  // =======================================================
  private toggleBookmark(): void {
    const active = this.tabStore.getActiveTab();
    if (!active || !this.browserStore) return;

    this.browserStore.toggleBookmark({
      id: String(Date.now()),
      title: active.title || 'New Tab',
      url: active.url,
      favicon: active.favicon
    });

    this.updateBookmarkStar();
  }

  private updateBookmarkStar(): void {
    const active = this.tabStore.getActiveTab();
    if (!this.starBtn || !active) return;

    const isBookmarked = this.browserStore ? this.browserStore.isBookmarked(active.url) : false;
    if (isBookmarked) {
      this.starBtn.className = 'omnibar-action-btn star-btn bookmarked';
      this.starBtn.title = 'Remove bookmark (Ctrl+D)';
      this.starBtn.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="#f59e0b" stroke="#f59e0b" stroke-width="2">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
        </svg>
      `;
    } else {
      this.starBtn.className = 'omnibar-action-btn star-btn';
      this.starBtn.title = 'Bookmark this tab (Ctrl+D)';
      this.starBtn.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
        </svg>
      `;
    }
  }

  // =======================================================
  // ZOOM INDICATOR & RESET
  // =======================================================
  private updateZoomBadge(): void {
    if (this.currentZoomLevel !== 100) {
      this.zoomBtn.textContent = `${this.currentZoomLevel}%`;
      this.zoomBtn.style.display = 'inline-flex';
    } else {
      this.zoomBtn.style.display = 'none';
    }
  }

  private resetZoom(): void {
    this.currentZoomLevel = 100;
    this.updateZoomBadge();
  }

  // =======================================================
  // SITE SECURITY & PAGE INFO (BraveShieldsPageInfoController)
  // =======================================================
  private togglePageInfoBubble(): void {
    if (this.isPageInfoOpen) {
      this.closePageInfoBubble();
    } else {
      this.openPageInfoBubble();
    }
  }

  private openPageInfoBubble(): void {
    this.closeDropdown();
    this.closeQrShareBubble();

    const active = this.tabStore.getActiveTab();
    let host = 'Local System';
    let isHttps = false;
    let isLocal = true;

    if (active && active.url) {
      if (active.url.startsWith('https://')) {
        host = new URL(active.url).hostname;
        isHttps = true;
        isLocal = false;
      } else if (active.url.startsWith('http://')) {
        host = new URL(active.url).hostname;
        isHttps = false;
        isLocal = false;
      } else if (active.url.startsWith('azokle://')) {
        host = 'Azokle Internal Core';
        isLocal = true;
      }
    }

    this.pageInfoBubbleEl.innerHTML = `
      <div class="page-info-header">
        <div class="page-info-icon-badge ${isHttps ? 'secure' : (isLocal ? 'local' : 'warning')}">
          ${this.getSecurityIcon(isHttps ? 'SECURE' : (isLocal ? 'LOCAL' : 'WARNING'))}
        </div>
        <div class="page-info-title-group">
          <div class="page-info-domain">${this.escapeHtml(host)}</div>
          <div class="page-info-subtext">
            ${isHttps ? 'Connection is secure (TLS 1.3)' : (isLocal ? 'Internal System Page' : 'Connection is not encrypted')}
          </div>
        </div>
      </div>

      <div class="page-info-body">
        <div class="page-info-row">
          <div class="page-info-label">
            <span class="info-dot ${isHttps ? 'green' : 'amber'}"></span>
            <span>Certificate Identity</span>
          </div>
          <div class="page-info-value">${isHttps ? 'Valid & Verified' : 'N/A'}</div>
        </div>
        <div class="page-info-row">
          <div class="page-info-label">
            <span class="info-dot green"></span>
            <span>Azokle Shield</span>
          </div>
          <div class="page-info-value">Aggressive Protection ON</div>
        </div>
        <div class="page-info-row">
          <div class="page-info-label">
            <span class="info-dot green"></span>
            <span>Cookies & Storage</span>
          </div>
          <div class="page-info-value">Isolated Partition</div>
        </div>
        <div class="page-info-row">
          <div class="page-info-label">
            <span class="info-dot green"></span>
            <span>Fingerprinting</span>
          </div>
          <div class="page-info-value">Canvas & Audio Spoofed</div>
        </div>
      </div>

      <div class="page-info-footer">
        <button class="page-info-btn" id="btn-close-pageinfo">Close</button>
      </div>
    `;

    this.pageInfoBubbleEl.querySelector('#btn-close-pageinfo')?.addEventListener('click', () => {
      this.closePageInfoBubble();
    });

    this.pageInfoBubbleEl.style.display = 'block';
    this.isPageInfoOpen = true;
  }

  private closePageInfoBubble(): void {
    this.pageInfoBubbleEl.style.display = 'none';
    this.isPageInfoOpen = false;
  }

  // =======================================================
  // QR CODE SHARING MODAL
  // =======================================================
  private toggleQrShareBubble(): void {
    if (this.isQrModalOpen) {
      this.closeQrShareBubble();
    } else {
      this.openQrShareBubble();
    }
  }

  private openQrShareBubble(): void {
    this.closeDropdown();
    this.closePageInfoBubble();

    const active = this.tabStore.getActiveTab();
    const url = active?.url || 'https://search.azokle.com';

    this.qrShareBubbleEl.innerHTML = `
      <div class="qr-share-header">
        <span class="qr-share-title">Scan QR code</span>
        <span class="qr-share-subtitle">Open this tab on your mobile device</span>
      </div>
      <div class="qr-code-display">
        <svg width="150" height="150" viewBox="0 0 100 100" class="qr-svg">
          <!-- Outer Position Marker Top Left -->
          <rect x="10" y="10" width="28" height="28" rx="4" fill="none" stroke="#f97316" stroke-width="5"/>
          <rect x="17" y="17" width="14" height="14" rx="2" fill="#f97316"/>

          <!-- Outer Position Marker Top Right -->
          <rect x="62" y="10" width="28" height="28" rx="4" fill="none" stroke="#f97316" stroke-width="5"/>
          <rect x="69" y="17" width="14" height="14" rx="2" fill="#f97316"/>

          <!-- Outer Position Marker Bottom Left -->
          <rect x="10" y="62" width="28" height="28" rx="4" fill="none" stroke="#f97316" stroke-width="5"/>
          <rect x="17" y="69" width="14" height="14" rx="2" fill="#f97316"/>

          <!-- Data Grid Pattern Modules -->
          <rect x="44" y="12" width="6" height="6" fill="#f1f5f9"/>
          <rect x="52" y="12" width="6" height="6" fill="#f1f5f9"/>
          <rect x="44" y="22" width="6" height="6" fill="#f1f5f9"/>
          <rect x="44" y="32" width="6" height="6" fill="#f1f5f9"/>
          <rect x="52" y="32" width="6" height="6" fill="#f1f5f9"/>

          <rect x="12" y="44" width="6" height="6" fill="#f1f5f9"/>
          <rect x="22" y="44" width="6" height="6" fill="#f1f5f9"/>
          <rect x="32" y="44" width="6" height="6" fill="#f1f5f9"/>
          <rect x="44" y="44" width="6" height="6" fill="#f97316"/>
          <rect x="52" y="44" width="6" height="6" fill="#f1f5f9"/>
          <rect x="64" y="44" width="6" height="6" fill="#f1f5f9"/>
          <rect x="74" y="44" width="6" height="6" fill="#f1f5f9"/>
          <rect x="84" y="44" width="6" height="6" fill="#f1f5f9"/>

          <rect x="44" y="54" width="6" height="6" fill="#f1f5f9"/>
          <rect x="54" y="54" width="6" height="6" fill="#f97316"/>
          <rect x="74" y="54" width="6" height="6" fill="#f1f5f9"/>

          <rect x="44" y="64" width="6" height="6" fill="#f1f5f9"/>
          <rect x="54" y="64" width="6" height="6" fill="#f1f5f9"/>
          <rect x="64" y="64" width="6" height="6" fill="#f1f5f9"/>
          <rect x="84" y="64" width="6" height="6" fill="#f1f5f9"/>

          <rect x="44" y="74" width="6" height="6" fill="#f1f5f9"/>
          <rect x="64" y="74" width="6" height="6" fill="#f97316"/>
          <rect x="74" y="74" width="6" height="6" fill="#f1f5f9"/>
          <rect x="84" y="74" width="6" height="6" fill="#f1f5f9"/>

          <rect x="44" y="84" width="6" height="6" fill="#f1f5f9"/>
          <rect x="54" y="84" width="6" height="6" fill="#f1f5f9"/>
          <rect x="74" y="84" width="6" height="6" fill="#f1f5f9"/>
        </svg>
      </div>
      <div class="qr-share-url" title="${this.escapeHtml(url)}">${this.escapeHtml(url)}</div>
    `;

    this.qrShareBubbleEl.style.display = 'block';
    this.isQrModalOpen = true;
  }

  private closeQrShareBubble(): void {
    this.qrShareBubbleEl.style.display = 'none';
    this.isQrModalOpen = false;
  }

  // =======================================================
  // STATE SYNC
  // =======================================================
  private updateState(): void {
    const activeTab = this.tabStore.getActiveTab();
    if (!activeTab) return;

    if (document.activeElement === this.inputEl) return;

    if (activeTab.url === 'azokle://newtab' || activeTab.url === 'about:blank') {
      this.inputEl.value = '';
      this.inputEl.placeholder = 'Search with Azokle or enter URL...';
      this.searchEngineBadgeEl.style.display = 'none';
      this.updateSecurityBadge('LOCAL');
    } else {
      this.inputEl.value = activeTab.url;
      this.updateSecurityBadge(activeTab.securityLevel);
    }

    this.updateBookmarkStar();
  }

  private updateSecurityBadge(level: SecurityLevel): void {
    this.securityBadgeEl.className = `security-badge ${level.toLowerCase()}`;
    this.securityBadgeEl.innerHTML = this.getSecurityIcon(level);
  }

  private getSecurityIcon(level: SecurityLevel): string {
    switch (level) {
      case 'SECURE':
        return `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>`;
      case 'WARNING':
      case 'INSECURE':
        return `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`;
      case 'LOCAL':
      default:
        return `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`;
    }
  }

  private navigateToUrl(url: string): void {
    this.tabStore.navigate(url);
  }

  private navigateToQuery(input: string): void {
    let resolved = input.trim();

    if (resolved.startsWith('!g ')) {
      resolved = `https://www.google.com/search?q=${encodeURIComponent(resolved.slice(3))}`;
    } else if (resolved.startsWith('!gh ')) {
      resolved = `https://github.com/search?q=${encodeURIComponent(resolved.slice(4))}`;
    } else if (resolved.startsWith('!yt ')) {
      resolved = `https://www.youtube.com/results?search_query=${encodeURIComponent(resolved.slice(4))}`;
    } else if (resolved.startsWith('!w ')) {
      resolved = `https://en.wikipedia.org/wiki/Special:Search?search=${encodeURIComponent(resolved.slice(3))}`;
    } else if (resolved.startsWith('!ddg ')) {
      resolved = `https://duckduckgo.com/?q=${encodeURIComponent(resolved.slice(5))}`;
    } else if (resolved.startsWith('http://') || resolved.startsWith('https://') || resolved.startsWith('azokle://') || resolved.startsWith('file://')) {
      // Direct protocol address
    } else if (resolved.includes('.') && !resolved.includes(' ')) {
      resolved = `https://${resolved}`;
    } else {
      resolved = `https://search.azokle.com/search?q=${encodeURIComponent(resolved)}`;
    }

    this.tabStore.navigate(resolved);
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }
}
