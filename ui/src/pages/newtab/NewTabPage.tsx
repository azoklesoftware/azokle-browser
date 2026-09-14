/**
 * Azokle Browser - Brave-Grade New Tab Page (React Component)
 * Inspired by Brave-Core's brave_new_tab_ui architecture
 */

import React, { useState, useEffect } from 'react';
import type { ShieldMetrics, BookmarkItem } from '../../bridge/types.ts';
import { getDesktopBridge } from '../../bridge/bridge.ts';

interface SpeedDialItem {
  id: string;
  title: string;
  url: string;
  icon: string;
}

export const NewTabPage: React.FC = () => {
  const bridge = getDesktopBridge();

  const [metrics, setMetrics] = useState<ShieldMetrics>(() => {
    try {
      if (typeof localStorage !== 'undefined') {
        const cached = localStorage.getItem('azokle_shield_metrics');
        if (cached) return JSON.parse(cached);
      }
    } catch {}
    return {
      totalBlocked: 0,
      adsBlocked: 0,
      trackersBlocked: 0,
      httpsUpgrades: 0,
      isShieldActive: true,
      fingerprintProtection: true,
      cookieBlocking: true
    };
  });

  const [currentTime, setCurrentTime] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isCustomizeOpen, setIsCustomizeOpen] = useState<boolean>(false);
  const [showStats, setShowStats] = useState<boolean>(true);
  const [showTopSites, setShowTopSites] = useState<boolean>(true);
  const [showClock, setShowClock] = useState<boolean>(true);
  const [showWallpaper, setShowWallpaper] = useState<boolean>(true);

  // Speed Dials with persistent local storage
  const [sites, setSites] = useState<SpeedDialItem[]>(() => {
    try {
      if (typeof localStorage !== 'undefined') {
        const saved = localStorage.getItem('azokle_top_sites');
        if (saved) return JSON.parse(saved);
      }
    } catch {}
    return [
      { id: '1', title: 'Azokle Search', url: 'https://search.azokle.com', icon: '🌐' },
      { id: '2', title: 'GitHub', url: 'https://github.com', icon: '🐙' },
      { id: '3', title: 'Wikipedia', url: 'https://en.wikipedia.org', icon: '📚' },
      { id: '4', title: 'Tor Onion Search', url: 'https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion', icon: '🧅' }
    ];
  });

  const [isAddSiteOpen, setIsAddSiteOpen] = useState(false);
  const [newSiteTitle, setNewSiteTitle] = useState('');
  const [newSiteUrl, setNewSiteUrl] = useState('');

  // Clock timer
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  // Fetch metrics and keep local storage synchronized
  useEffect(() => {
    bridge.getShieldMetrics().then((m) => {
      if (m) {
        setMetrics(m);
        try {
          if (typeof localStorage !== 'undefined') {
            localStorage.setItem('azokle_shield_metrics', JSON.stringify(m));
          }
        } catch {}
      }
    }).catch(() => {});

    const listener = (m: ShieldMetrics) => {
      if (m) {
        setMetrics(m);
        try {
          if (typeof localStorage !== 'undefined') {
            localStorage.setItem('azokle_shield_metrics', JSON.stringify(m));
          }
        } catch {}
      }
    };
    bridge.on('shield-metrics-updated', listener);
    return () => bridge.off('shield-metrics-updated', listener);
  }, []);

  // Calculated estimates:
  // Bandwidth saved ~ 125 KB per blocked tracker/ad
  // Time saved ~ 45 ms per blocked tracker/ad
  const bandwidthSavedMB = ((metrics.totalBlocked * 125) / 1024).toFixed(1);
  const timeSavedSec = ((metrics.totalBlocked * 45) / 1000).toFixed(1);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const q = searchQuery.trim();
    if (!q) return;

    if (q.startsWith('http://') || q.startsWith('https://')) {
      bridge.navigate(q);
    } else if (q.includes('.') && !q.includes(' ')) {
      bridge.navigate(`https://${q}`);
    } else {
      bridge.navigate(`https://search.azokle.com/search?q=${encodeURIComponent(q)}`);
    }
  };

  const handleAddSite = () => {
    if (!newSiteTitle.trim() || !newSiteUrl.trim()) return;
    let url = newSiteUrl.trim();
    if (!url.startsWith('http')) url = `https://${url}`;

    const updated = [
      ...sites,
      { id: String(Date.now()), title: newSiteTitle.trim(), url, icon: '⭐' }
    ];
    setSites(updated);
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('azokle_top_sites', JSON.stringify(updated));
      }
    } catch {}
    setNewSiteTitle('');
    setNewSiteUrl('');
    setIsAddSiteOpen(false);
  };

  return (
    <div className={`brave-ntp-root ${showWallpaper ? 'with-wallpaper' : 'solid-dark'}`}>
      {/* 1. Hero Privacy Stats Counter (Brave-style) */}
      {showStats && (
        <div className="brave-ntp-stats-header">
          <div className="stat-metric-block">
            <div className="stat-metric-number orange">{metrics.totalBlocked.toLocaleString()}</div>
            <div className="stat-metric-label">Trackers & ads blocked</div>
          </div>
          <div className="stat-metric-block">
            <div className="stat-metric-number cyan">{bandwidthSavedMB} <span className="unit">MB</span></div>
            <div className="stat-metric-label">Bandwidth saved</div>
          </div>
          <div className="stat-metric-block">
            <div className="stat-metric-number emerald">{timeSavedSec} <span className="unit">s</span></div>
            <div className="stat-metric-label">Time saved</div>
          </div>
        </div>
      )}

      {/* 2. Central Clock & Search Area */}
      <div className="brave-ntp-center">
        {showClock && <div className="brave-ntp-clock">{currentTime}</div>}

        <form className="brave-ntp-search-capsule" onSubmit={handleSearchSubmit}>
          <span className="search-engine-icon">🌐</span>
          <input
            type="text"
            className="search-capsule-input"
            placeholder="Search with Azokle or type a URL..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            autoFocus
          />
          <button type="submit" className="search-submit-btn">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </button>
        </form>

        {/* 3. Top Sites Grid (Speed Dials) */}
        {showTopSites && (
          <div className="brave-ntp-top-sites-grid">
            {sites.map((site) => (
              <a
                key={site.id}
                href={site.url}
                className="top-site-tile"
                onClick={(e) => {
                  e.preventDefault();
                  bridge.navigate(site.url);
                }}
              >
                <div className="top-site-icon-box">{site.icon}</div>
                <div className="top-site-title">{site.title}</div>
              </a>
            ))}

            {/* Add Site Button */}
            <button
              className="top-site-tile add-tile"
              onClick={() => setIsAddSiteOpen(true)}
              title="Add Shortcut"
            >
              <div className="top-site-icon-box add-icon">＋</div>
              <div className="top-site-title">Add Site</div>
            </button>
          </div>
        )}
      </div>

      {/* 4. Wallpaper Photographer Credit */}
      {showWallpaper && (
        <div className="wallpaper-credit-pill">
          <span>Photo by <b>Azokle Collection</b></span>
        </div>
      )}

      {/* 5. Floating Customize Dashboard Button */}
      <button
        className="customize-dashboard-btn"
        onClick={() => setIsCustomizeOpen(!isCustomizeOpen)}
        title="Customize Dashboard"
      >
        ⚙️ Customize
      </button>

      {/* Customize Drawer */}
      {isCustomizeOpen && (
        <div className="customize-drawer">
          <div className="customize-drawer-header">
            <h3>Customize Dashboard</h3>
            <button className="close-drawer-btn" onClick={() => setIsCustomizeOpen(false)}>✕</button>
          </div>
          <div className="customize-toggle-list">
            <label className="customize-row">
              <span>Background Image</span>
              <input
                type="checkbox"
                checked={showWallpaper}
                onChange={(e) => setShowWallpaper(e.target.checked)}
              />
            </label>
            <label className="customize-row">
              <span>Brave-Grade Privacy Stats</span>
              <input
                type="checkbox"
                checked={showStats}
                onChange={(e) => setShowStats(e.target.checked)}
              />
            </label>
            <label className="customize-row">
              <span>Top Sites Speed Dials</span>
              <input
                type="checkbox"
                checked={showTopSites}
                onChange={(e) => setShowTopSites(e.target.checked)}
              />
            </label>
            <label className="customize-row">
              <span>Clock</span>
              <input
                type="checkbox"
                checked={showClock}
                onChange={(e) => setShowClock(e.target.checked)}
              />
            </label>
          </div>
        </div>
      )}

      {/* Add Site Dialog */}
      {isAddSiteOpen && (
        <div className="modal-backdrop" onClick={() => setIsAddSiteOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <h3>Add Top Site Shortcut</h3>
            <div className="modal-form-group">
              <label>Site Name</label>
              <input
                type="text"
                placeholder="e.g. GitHub"
                value={newSiteTitle}
                onChange={(e) => setNewSiteTitle(e.target.value)}
                autoFocus
              />
            </div>
            <div className="modal-form-group">
              <label>Web Address (URL)</label>
              <input
                type="text"
                placeholder="https://..."
                value={newSiteUrl}
                onChange={(e) => setNewSiteUrl(e.target.value)}
              />
            </div>
            <div className="modal-actions">
              <button className="btn-cancel" onClick={() => setIsAddSiteOpen(false)}>Cancel</button>
              <button className="btn-confirm" onClick={handleAddSite}>Save</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
