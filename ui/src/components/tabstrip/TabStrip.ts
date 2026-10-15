/**
 * Azokle Browser - Shared Desktop UI
 * Top Tab Strip Component (Brave-Grade Architecture)
 */

import type { TabStore } from '../../state/tabStore.ts';
import type { TabInfo } from '../../bridge/types.ts';

export class TabStripComponent {
  private container: HTMLElement;
  private tabStore: TabStore;

  constructor(container: HTMLElement, tabStore: TabStore) {
    this.container = container;
    this.tabStore = tabStore;
    this.tabStore.subscribe(() => this.render());
    this.render();
  }

  private render(): void {
    const tabs = this.tabStore.getTabs();
    const activeId = this.tabStore.getActiveTabId();

    this.container.innerHTML = '';
    this.container.className = 'tabstrip-container';

    // Separate pinned and unpinned tabs
    const pinnedTabs = tabs.filter(t => t.isPinned);
    const regularTabs = tabs.filter(t => !t.isPinned);

    // 1. Render pinned tabs
    for (const tab of pinnedTabs) {
      this.container.appendChild(this.createTabElement(tab, tab.id === activeId, true));
    }

    // 2. Render regular tabs
    for (const tab of regularTabs) {
      this.container.appendChild(this.createTabElement(tab, tab.id === activeId, false));
    }

    // 3. Add Tab Button "+"
    const addBtn = document.createElement('button');
    addBtn.className = 'add-tab-btn';
    addBtn.title = 'New Tab (Ctrl+T)';
    addBtn.setAttribute('aria-label', 'New Tab');
    addBtn.innerHTML = `
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <line x1="12" y1="5" x2="12" y2="19"/>
        <line x1="5" y1="12" x2="19" y2="12"/>
      </svg>
    `;
    addBtn.onclick = () => this.tabStore.openTab('azokle://newtab');
    this.container.appendChild(addBtn);
  }

  private createTabElement(tab: TabInfo, isActive: boolean, isPinned: boolean): HTMLElement {
    const el = document.createElement('div');
    const classes = [
      'tab-item',
      isActive ? 'active' : '',
      isPinned ? 'pinned' : '',
      tab.isPrivate ? 'private' : '',
      tab.isTor ? 'tor' : ''
    ].filter(Boolean);

    el.className = classes.join(' ');
    el.dataset.tabId = String(tab.id);
    el.title = tab.title || 'New Tab';

    // Favicon / Status icon
    const iconEl = document.createElement('span');
    iconEl.className = 'tab-icon';
    if (tab.isLoading) {
      iconEl.innerHTML = `<div class="tab-spinner"></div>`;
    } else if (tab.isTor) {
      iconEl.textContent = '🧅';
    } else if (tab.isPrivate) {
      iconEl.textContent = '🕶️';
    } else if (tab.favicon && typeof tab.favicon === 'string') {
      try {
        const u = new URL(tab.favicon, window.location.href);
        if (u.protocol === 'https:' || u.protocol === 'http:' || (u.protocol === 'data:' && u.href.startsWith('data:image/'))) {
          const img = document.createElement('img');
          img.src = u.href;
          img.width = 14;
          img.height = 14;
          img.alt = '';
          iconEl.replaceChildren(img);
        } else {
          iconEl.textContent = tab.favicon.slice(0, 4);
        }
      } catch (_e) {
        iconEl.textContent = tab.favicon.slice(0, 4);
      }
    } else {
      iconEl.innerHTML = `
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="2" y1="12" x2="22" y2="12"/>
          <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
        </svg>
      `;
    }
    el.appendChild(iconEl);

    // If not pinned, add Title, Audio indicator, and Close button
    if (!isPinned) {
      // Title label
      const titleEl = document.createElement('span');
      titleEl.className = 'tab-title';
      titleEl.textContent = tab.title || 'New Tab';
      el.appendChild(titleEl);

      // Audio playing / mute toggle
      if (tab.isAudioPlaying) {
        const audioBtn = document.createElement('button');
        audioBtn.className = `tab-audio-btn ${tab.isMuted ? 'muted' : 'playing'}`;
        audioBtn.title = tab.isMuted ? 'Unmute Tab' : 'Mute Tab';
        audioBtn.innerHTML = tab.isMuted
          ? `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>`
          : `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>`;
        audioBtn.onclick = (e) => {
          e.stopPropagation();
          tab.isMuted = !tab.isMuted;
          this.render();
        };
        el.appendChild(audioBtn);
      }

      // Close button
      const closeBtn = document.createElement('button');
      closeBtn.className = 'tab-close-btn';
      closeBtn.title = 'Close Tab (Ctrl+W)';
      closeBtn.setAttribute('aria-label', 'Close Tab');
      closeBtn.innerHTML = `
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      `;
      closeBtn.onclick = (e) => {
        e.stopPropagation();
        this.tabStore.closeTab(tab.id);
      };
      el.appendChild(closeBtn);
    }

    // Switch tab on click
    el.onclick = () => this.tabStore.selectTab(tab.id);

    return el;
  }
}
