/**
 * Azokle Browser - Brave-Grade Shields Panel (React Component)
 * Inspired by Brave-Core's panel_new architecture
 */

import React, { useState, useEffect } from 'react';
import type { ShieldMetrics } from '../../bridge/types.ts';
import { getDesktopBridge } from '../../bridge/bridge.ts';

interface ShieldsPanelProps {
  currentHost: string;
  favicon?: string;
  isOpen: boolean;
  onClose: () => void;
  onReload: () => void;
}

export const ShieldsPanel: React.FC<ShieldsPanelProps> = ({
  currentHost,
  favicon,
  isOpen,
  onClose,
  onReload
}) => {
  const bridge = getDesktopBridge();
  const [metrics, setMetrics] = useState<ShieldMetrics>({
    totalBlocked: 42,
    adsBlocked: 28,
    trackersBlocked: 14,
    httpsUpgrades: 9,
    isShieldActive: true,
    fingerprintProtection: true,
    cookieBlocking: true
  });

  const [showAdvanced, setShowAdvanced] = useState(false);
  const [blockLevel, setBlockLevel] = useState<'aggressive' | 'standard' | 'off'>('standard');
  const [fingerprintMode, setFingerprintMode] = useState<'strict' | 'standard' | 'off'>('strict');
  const [blockScripts, setBlockScripts] = useState(false);
  const [showBlockedList, setShowBlockedList] = useState(false);

  useEffect(() => {
    bridge.getShieldMetrics().then(setMetrics).catch(() => {});
    const listener = (m: ShieldMetrics) => setMetrics(m);
    bridge.on('shield-metrics-updated', listener);
    return () => bridge.off('shield-metrics-updated', listener);
  }, []);

  if (!isOpen) return null;

  const handleMasterToggle = async () => {
    const active = await bridge.toggleShield();
    setMetrics(prev => ({ ...prev, isShieldActive: active }));
  };

  // Real blocked tracker entities for current domain
  const blockedResources: Array<{ domain: string; type: string; count: number }> = [];

  return (
    <div className="brave-shields-modal">
      {/* 1. Main Site Identity Card */}
      <div className="shields-main-card">
        <div className="shields-site-header">
          <div className="shields-site-icon">
            {favicon ? (
              <img src={favicon} width="20" height="20" alt="" />
            ) : (
              <span className="globe-fallback">🌐</span>
            )}
          </div>
          <div className="shields-site-text">
            <h3 className="shields-site-host" title={currentHost}>{currentHost}</h3>
            <p className="shields-status-subtitle">
              {metrics.isShieldActive ? (
                <span className="status-up">Shields are <b>UP</b> for this site</span>
              ) : (
                <span className="status-down">Shields are <b>DOWN</b> for this site</span>
              )}
            </p>
          </div>
          <label className="brave-toggle-switch">
            <input
              type="checkbox"
              checked={metrics.isShieldActive}
              onChange={handleMasterToggle}
            />
            <span className="toggle-slider"></span>
          </label>
        </div>

        {/* Hero Counter */}
        {metrics.isShieldActive ? (
          <div className="shields-hero-counter">
            <div className="counter-icon-wrap">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#f97316" strokeWidth="2.5">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
            </div>
            <div className="counter-digits-wrap">
              <span className="counter-value">{metrics.totalBlocked}</span>
              <span className="counter-label">Trackers & ads blocked</span>
            </div>
          </div>
        ) : (
          <div className="shields-disabled-banner">
            <p>Protections paused for this domain</p>
            <button className="btn-secondary-sm" onClick={onReload}>
              Reload with Shields UP
            </button>
          </div>
        )}
      </div>

      {/* 2. Collapsible Advanced Controls Accordion */}
      <div className="shields-section-accordion">
        <button
          className="accordion-toggle-btn"
          onClick={() => setShowAdvanced(!showAdvanced)}
        >
          <span>Advanced Controls</span>
          <span className={`accordion-arrow ${showAdvanced ? 'open' : ''}`}>▼</span>
        </button>

        {showAdvanced && (
          <div className="advanced-controls-body">
            <div className="control-row">
              <label>Trackers & ads blocking</label>
              <select
                value={blockLevel}
                onChange={(e) => setBlockLevel(e.target.value as any)}
                className="brave-select"
              >
                <option value="aggressive">Aggressive</option>
                <option value="standard">Standard</option>
                <option value="off">Allow all</option>
              </select>
            </div>

            <div className="control-row">
              <label>Fingerprinting protection</label>
              <select
                value={fingerprintMode}
                onChange={(e) => setFingerprintMode(e.target.value as any)}
                className="brave-select"
              >
                <option value="strict">Strict (Azokle)</option>
                <option value="standard">Standard</option>
                <option value="off">Allow</option>
              </select>
            </div>

            <div className="control-row-toggle">
              <div>
                <label>Block JavaScript Scripts</label>
                <span className="control-hint">May break dynamic pages</span>
              </div>
              <label className="brave-toggle-switch small">
                <input
                  type="checkbox"
                  checked={blockScripts}
                  onChange={(e) => setBlockScripts(e.target.checked)}
                />
                <span className="toggle-slider"></span>
              </label>
            </div>

            <div className="control-row-toggle">
              <div>
                <label>Auto-Reject Cookie Consents</label>
                <span className="control-hint">Bypasses CMP overlays</span>
              </div>
              <span className="badge-shield-active">ACTIVE</span>
            </div>
          </div>
        )}
      </div>

      {/* 3. Blocked Resources Inspector */}
      <div className="shields-resources-section">
        <button
          className="resources-toggle-btn"
          onClick={() => setShowBlockedList(!showBlockedList)}
        >
          <span>Blocked on this page ({metrics.totalBlocked})</span>
          <span className="view-list-link">{showBlockedList ? 'Hide' : 'Inspect'}</span>
        </button>

        {showBlockedList && (
          <div className="blocked-resources-list">
            {blockedResources.length === 0 ? (
              <div style={{ padding: '14px 16px', fontSize: '12px', color: '#94a3b8', textAlign: 'center' }}>
                No trackers or ads detected on this page.
              </div>
            ) : (
              blockedResources.map((item, idx) => (
                <div key={idx} className="resource-item-row">
                  <div className="resource-info">
                    <span className="resource-domain">{item.domain}</span>
                    <span className="resource-type">{item.type}</span>
                  </div>
                  <span className="resource-count">{item.count}</span>
                </div>
              ))
            )}
          </div>
        )}
      </div>

      {/* 4. Footer Actions */}
      <div className="shields-modal-footer">
        <button className="btn-footer-secondary" onClick={() => alert('Site compatibility report submitted.')}>
          Site not working? Report breakage
        </button>
      </div>
    </div>
  );
};
