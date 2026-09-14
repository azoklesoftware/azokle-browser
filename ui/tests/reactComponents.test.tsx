/**
 * Unit tests for Brave-Grade React UI Components
 * Validates SSR rendering and component contracts
 */

import { describe, it, expect } from 'bun:test';
import React from 'react';
import { renderToString } from 'react-dom/server';
import { ShieldsPanel } from '../src/components/shield/ShieldsPanel.tsx';
import { NewTabPage } from '../src/pages/newtab/NewTabPage.tsx';

describe('Brave-Grade ShieldsPanel Component', () => {
  it('should render shields panel when open with site host and active status', () => {
    const html = renderToString(
      <ShieldsPanel
        currentHost="privacy-tests.azokle.com"
        isOpen={true}
        onClose={() => {}}
        onReload={() => {}}
      />
    );

    expect(html).toContain('privacy-tests.azokle.com');
    expect(html).toContain('Shields are');
    expect(html).toContain('UP');
    expect(html).toContain('Trackers &amp; ads blocked');
    expect(html).toContain('Advanced Controls');
  });

  it('should not render anything when isOpen is false', () => {
    const html = renderToString(
      <ShieldsPanel
        currentHost="privacy-tests.azokle.com"
        isOpen={false}
        onClose={() => {}}
        onReload={() => {}}
      />
    );

    expect(html).toBe('');
  });

  it('should include privacy control options matching Brave standards', () => {
    const html = renderToString(
      <ShieldsPanel
        currentHost="news.ycombinator.com"
        isOpen={true}
        onClose={() => {}}
        onReload={() => {}}
      />
    );

    expect(html).toContain('Advanced Controls');
    expect(html).toContain('Blocked on this page');
    expect(html).toContain('Report breakage');
  });
});

describe('Brave-Grade NewTabPage Component', () => {
  it('should render hero privacy metrics bar (Trackers, Bandwidth, Time saved)', () => {
    const html = renderToString(<NewTabPage />);

    expect(html).toContain('Trackers &amp; ads blocked');
    expect(html).toContain('Bandwidth saved');
    expect(html).toContain('Time saved');
    expect(html).toContain('MB');
    expect(html).toContain('s');
  });

  it('should render default speed dial sites', () => {
    const html = renderToString(<NewTabPage />);

    expect(html).toContain('Azokle Search');
    expect(html).toContain('GitHub');
    expect(html).toContain('Wikipedia');
    expect(html).toContain('Tor Onion Search');
    expect(html).toContain('Add Site');
  });

  it('should render sovereign search bar and branding', () => {
    const html = renderToString(<NewTabPage />);

    expect(html).toContain('Search with Azokle or type a URL...');
    expect(html).toContain('Azokle Search');
    expect(html).toContain('Customize');
  });
});
