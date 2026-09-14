/**
 * Azokle Browser - Shared Desktop UI
 * Bookmarks Bar Component (Brave-Grade Architecture)
 */

import type { BrowserStore } from '../../state/browserStore.ts';
import type { TabStore } from '../../state/tabStore.ts';

export class BookmarksBarComponent {
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
    const bookmarks = this.browserStore.getBookmarks();
    this.container.className = 'bookmarks-bar-container';
    this.container.innerHTML = '';

    if (bookmarks.length === 0) {
      // Empty state with import prompt matching Brave
      const emptyPrompt = document.createElement('div');
      emptyPrompt.className = 'bookmarks-empty-prompt';
      emptyPrompt.innerHTML = `
        <span>For quick access, place your bookmarks here on the bookmarks bar.</span>
        <button class="import-bookmarks-btn" id="btn-import-bookmarks">Import bookmarks now...</button>
      `;
      emptyPrompt.querySelector('#btn-import-bookmarks')?.addEventListener('click', () => {
        this.tabStore.navigate('azokle://settings');
      });
      this.container.appendChild(emptyPrompt);
      return;
    }

    // Bookmarks list
    const listWrapper = document.createElement('div');
    listWrapper.className = 'bookmarks-list-wrapper';

    for (const b of bookmarks) {
      const btn = document.createElement('button');
      btn.className = 'bookmark-pill';
      btn.title = `${b.title}\n${b.url}`;

      const isUrl = b.favicon && (b.favicon.startsWith('http') || b.favicon.startsWith('data:') || b.favicon.startsWith('/'));
      const iconContent = isUrl
        ? `<img src="${b.favicon}" width="14" height="14" alt="" />`
        : `<span class="bookmark-emoji">${b.favicon || '★'}</span>`;

      btn.innerHTML = `
        <span class="bookmark-icon-box">${iconContent}</span>
        <span class="bookmark-label">${this.escapeHtml(b.title)}</span>
      `;
      btn.onclick = () => this.tabStore.navigate(b.url);
      listWrapper.appendChild(btn);
    }

    // "All Bookmarks" folder button on the far right
    const allBookmarksBtn = document.createElement('button');
    allBookmarksBtn.className = 'bookmark-pill all-bookmarks-btn';
    allBookmarksBtn.title = 'All Bookmarks';
    allBookmarksBtn.innerHTML = `
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
      <span>All Bookmarks</span>
    `;
    allBookmarksBtn.onclick = () => this.tabStore.navigate('azokle://bookmarks');

    this.container.appendChild(listWrapper);
    this.container.appendChild(allBookmarksBtn);
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }
}
